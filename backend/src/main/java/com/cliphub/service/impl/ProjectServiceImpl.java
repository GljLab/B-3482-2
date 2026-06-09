package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.CollaboratorRequest;
import com.cliphub.dto.ProjectCreateRequest;
import com.cliphub.dto.SaveVersionRequest;
import com.cliphub.entity.*;
import com.cliphub.mapper.*;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import com.cliphub.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMaterialRelMapper projectMaterialRelMapper;
    private final ProjectVersionMapper projectVersionMapper;
    private final ProjectCollaboratorMapper projectCollaboratorMapper;
    private final MaterialMapper materialMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final CollectionMaterialRelMapper collectionMaterialRelMapper;
    @Lazy
    private final TraceabilityServiceImpl traceabilitySvc;

    @Value("${app.storage.root}")
    private String storageRoot;

    @Override
    @Transactional
    public Map<String, Object> createProject(UserPrincipal principal, ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwnerId(principal.getId());
        project.setStatus("DRAFT");
        project.setTeamId(request.getTeamId() == null ? principal.getTeamId() : request.getTeamId());
        project.setExportFormat(request.getExportFormat() == null ? "mp4" : request.getExportFormat());
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);

        if (request.getMaterialIds() != null) {
            for (Long materialId : request.getMaterialIds()) {
                bindMaterial(principal, project.getId(), materialId);
            }
        }

        ProjectVersion initVersion = new ProjectVersion();
        initVersion.setProjectId(project.getId());
        initVersion.setVersionNo(1);
        initVersion.setVersionName("v1-initial");
        initVersion.setContentJson("{\"tracks\":[],\"timeline\":[]}");
        initVersion.setCreatedBy(principal.getId());
        initVersion.setIsCurrent(1);
        initVersion.setCreatedAt(LocalDateTime.now());
        projectVersionMapper.insert(initVersion);

        project.setCurrentVersionId(initVersion.getId());
        projectMapper.updateById(project);

        traceabilitySvc.saveVersionMaterialSnapshot(initVersion.getId(), project.getId());

        auditLogService.log(principal, "CREATE_PROJECT", "PROJECT", String.valueOf(project.getId()), "创建项目");
        return projectToMap(project);
    }

    @Override
    public List<Map<String, Object>> listProjects(UserPrincipal principal) {
        List<Project> projects;
        if ("ADMIN".equals(principal.getRole())) {
            projects = projectMapper.selectList(new LambdaQueryWrapper<Project>().orderByDesc(Project::getUpdatedAt));
        } else {
            List<ProjectCollaborator> collabs = projectCollaboratorMapper.selectList(new LambdaQueryWrapper<ProjectCollaborator>()
                    .eq(ProjectCollaborator::getUserId, principal.getId()));
            Set<Long> ids = collabs.stream().map(ProjectCollaborator::getProjectId).collect(Collectors.toSet());
            ids.add(-1L);
            projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                    .and(w -> w.eq(Project::getOwnerId, principal.getId()).or().in(Project::getId, ids))
                    .orderByDesc(Project::getUpdatedAt));
        }
        return projects.stream().map(this::projectToMap).toList();
    }

    @Override
    @Transactional
    public Map<String, Object> saveVersion(UserPrincipal principal, Long projectId, SaveVersionRequest request) {
        Project project = mustProject(projectId);
        ensureCanEdit(principal, projectId, project);

        List<ProjectVersion> versions = projectVersionMapper.selectList(new LambdaQueryWrapper<ProjectVersion>()
                .eq(ProjectVersion::getProjectId, projectId)
                .orderByDesc(ProjectVersion::getVersionNo));

        int nextNo = versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;

        projectVersionMapper.update(null, new LambdaUpdateWrapper<ProjectVersion>()
                .set(ProjectVersion::getIsCurrent, 0)
                .eq(ProjectVersion::getProjectId, projectId));

        ProjectVersion version = new ProjectVersion();
        version.setProjectId(projectId);
        version.setVersionNo(nextNo);
        version.setVersionName(request.getVersionName());
        version.setContentJson(request.getContentJson());
        version.setCreatedBy(principal.getId());
        version.setIsCurrent(1);
        version.setCreatedAt(LocalDateTime.now());
        projectVersionMapper.insert(version);

        project.setCurrentVersionId(version.getId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        traceabilitySvc.saveVersionMaterialSnapshot(version.getId(), projectId);

        auditLogService.log(principal, "SAVE_PROJECT_VERSION", "PROJECT", String.valueOf(projectId),
                "保存版本: " + request.getVersionName());

        return Map.of(
                "projectId", projectId,
                "versionId", version.getId(),
                "versionNo", version.getVersionNo(),
                "versionName", version.getVersionName()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> rollback(UserPrincipal principal, Long projectId, Long versionId) {
        Project project = mustProject(projectId);
        ensureCanEdit(principal, projectId, project);

        ProjectVersion target = projectVersionMapper.selectById(versionId);
        if (target == null || !Objects.equals(target.getProjectId(), projectId)) {
            throw new BusinessException("目标版本不存在");
        }

        projectVersionMapper.update(null, new LambdaUpdateWrapper<ProjectVersion>()
                .set(ProjectVersion::getIsCurrent, 0)
                .eq(ProjectVersion::getProjectId, projectId));
        projectVersionMapper.update(null, new LambdaUpdateWrapper<ProjectVersion>()
                .set(ProjectVersion::getIsCurrent, 1)
                .eq(ProjectVersion::getId, target.getId()));

        project.setCurrentVersionId(target.getId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        auditLogService.log(principal, "ROLLBACK_PROJECT_VERSION", "PROJECT", String.valueOf(projectId),
                "回滚到版本 " + target.getVersionName());
        return Map.of("projectId", projectId, "currentVersionId", target.getId(), "versionName", target.getVersionName());
    }

    @Override
    @Transactional
    public Map<String, Object> exportProject(UserPrincipal principal, Long projectId, String format) {
        Project project = mustProject(projectId);
        ensureCanView(principal, projectId, project);

        ProjectVersion version = project.getCurrentVersionId() == null
                ? null : projectVersionMapper.selectById(project.getCurrentVersionId());
        if (version == null) {
            throw new BusinessException("项目无可导出版本");
        }

        String exportFormat = (format == null || format.isBlank())
                ? project.getExportFormat()
                : format;

        Path exportDir = Paths.get(storageRoot, "exports", String.valueOf(projectId));
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建导出目录失败");
        }

        String fileName = "project-" + projectId + "-v" + version.getVersionNo() + "." + exportFormat;
        Path exportPath = exportDir.resolve(fileName);
        try {
            Files.writeString(exportPath, version.getContentJson());
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出失败");
        }

        project.setStatus("ACTIVE");
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        auditLogService.log(principal, "EXPORT_PROJECT", "PROJECT", String.valueOf(projectId), "导出项目格式: " + exportFormat);
        return Map.of(
                "projectId", projectId,
                "format", exportFormat,
                "path", exportPath.toString(),
                "downloadUrl", "/api/projects/" + projectId + "/exports?path=" + fileName
        );
    }

    @Override
    @Transactional
    public Map<String, Object> addCollaborator(UserPrincipal principal, Long projectId, CollaboratorRequest request) {
        Project project = mustProject(projectId);
        ensureCanEdit(principal, projectId, project);

        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("协作用户不存在");
        }

        ProjectCollaborator existed = projectCollaboratorMapper.selectOne(new LambdaQueryWrapper<ProjectCollaborator>()
                .eq(ProjectCollaborator::getProjectId, projectId)
                .eq(ProjectCollaborator::getUserId, request.getUserId())
                .last("LIMIT 1"));

        if (existed == null) {
            ProjectCollaborator collaborator = new ProjectCollaborator();
            collaborator.setProjectId(projectId);
            collaborator.setUserId(request.getUserId());
            collaborator.setRole(request.getRole().toUpperCase(Locale.ROOT));
            collaborator.setCreatedAt(LocalDateTime.now());
            projectCollaboratorMapper.insert(collaborator);
        } else {
            existed.setRole(request.getRole().toUpperCase(Locale.ROOT));
            projectCollaboratorMapper.updateById(existed);
        }

        auditLogService.log(principal, "ADD_PROJECT_COLLABORATOR", "PROJECT", String.valueOf(projectId),
                "添加协作人: " + request.getUserId());

        return Map.of("projectId", projectId, "userId", request.getUserId(), "role", request.getRole().toUpperCase(Locale.ROOT));
    }

    @Override
    @Transactional
    public void removeCollaborator(UserPrincipal principal, Long projectId, Long userId) {
        Project project = mustProject(projectId);
        ensureCanEdit(principal, projectId, project);

        projectCollaboratorMapper.delete(new LambdaQueryWrapper<ProjectCollaborator>()
                .eq(ProjectCollaborator::getProjectId, projectId)
                .eq(ProjectCollaborator::getUserId, userId));

        auditLogService.log(principal, "REMOVE_PROJECT_COLLABORATOR", "PROJECT", String.valueOf(projectId),
                "移除协作人: " + userId);
    }

    @Override
    public List<Map<String, Object>> listVersions(UserPrincipal principal, Long projectId) {
        Project project = mustProject(projectId);
        ensureCanView(principal, projectId, project);

        return projectVersionMapper.selectList(new LambdaQueryWrapper<ProjectVersion>()
                        .eq(ProjectVersion::getProjectId, projectId)
                        .orderByDesc(ProjectVersion::getVersionNo))
                .stream()
                .map(version -> Map.<String, Object>of(
                        "id", version.getId(),
                        "versionNo", version.getVersionNo(),
                        "versionName", version.getVersionName(),
                        "isCurrent", version.getIsCurrent(),
                        "createdAt", version.getCreatedAt(),
                        "createdBy", version.getCreatedBy()))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> bindMaterial(UserPrincipal principal, Long projectId, Long materialId) {
        Project project = mustProject(projectId);
        ensureCanEdit(principal, projectId, project);

        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException("素材不存在");
        }
        if (!canAccessMaterial(principal, material)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "素材不可绑定到该项目");
        }

        ProjectMaterialRel existed = projectMaterialRelMapper.selectOne(new LambdaQueryWrapper<ProjectMaterialRel>()
                .eq(ProjectMaterialRel::getProjectId, projectId)
                .eq(ProjectMaterialRel::getMaterialId, materialId)
                .last("LIMIT 1"));
        if (existed == null) {
            ProjectMaterialRel relation = new ProjectMaterialRel();
            relation.setProjectId(projectId);
            relation.setMaterialId(materialId);
            relation.setCreatedAt(LocalDateTime.now());
            relation.setAddedBy(principal.getId());
            relation.setAddedAt(LocalDateTime.now());
            relation.setSourceType("DIRECT_BIND");
            relation.setMaterialOwnerId(material.getOwnerId());
            relation.setMaterialStatus("ACTIVE");
            projectMaterialRelMapper.insert(relation);
            auditLogService.log(principal, "BIND_PROJECT_MATERIAL", "PROJECT", String.valueOf(projectId),
                    "绑定素材: " + materialId);
            traceabilitySvc.logTrail(materialId, principal, "BIND_TO_PROJECT", "PROJECT_USAGE", "PROJECT", String.valueOf(projectId), project.getName(), null, null);
            traceabilitySvc.updateMaterialCounters(materialId);
        }

        return Map.of("projectId", projectId, "materialId", materialId, "bound", true);
    }

    @Override
    @Transactional
    public Map<String, Object> importFromCollection(UserPrincipal principal, Long projectId, Long collectionId) {
        Project project = mustProject(projectId);
        ensureCanEdit(principal, projectId, project);

        List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>()
                        .eq(CollectionMaterialRel::getCollectionId, collectionId));

        List<Long> newlyBoundMaterialIds = new ArrayList<>();
        int boundCount = 0;
        for (CollectionMaterialRel rel : rels) {
            Material material = materialMapper.selectById(rel.getMaterialId());
            if (material == null || !canAccessMaterial(principal, material)) {
                continue;
            }

            ProjectMaterialRel existed = projectMaterialRelMapper.selectOne(new LambdaQueryWrapper<ProjectMaterialRel>()
                    .eq(ProjectMaterialRel::getProjectId, projectId)
                    .eq(ProjectMaterialRel::getMaterialId, rel.getMaterialId())
                    .last("LIMIT 1"));
            if (existed == null) {
                ProjectMaterialRel relation = new ProjectMaterialRel();
                relation.setProjectId(projectId);
                relation.setMaterialId(rel.getMaterialId());
                relation.setCreatedAt(LocalDateTime.now());
                relation.setAddedBy(principal.getId());
                relation.setAddedAt(LocalDateTime.now());
                relation.setSourceType("COLLECTION_IMPORT");
                relation.setSourceNote("从素材集" + collectionId + "导入");
                relation.setMaterialOwnerId(material.getOwnerId());
                relation.setMaterialStatus("ACTIVE");
                projectMaterialRelMapper.insert(relation);
                newlyBoundMaterialIds.add(rel.getMaterialId());
                boundCount++;
            }
        }

        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        auditLogService.log(principal, "IMPORT_COLLECTION_TO_PROJECT", "PROJECT", String.valueOf(projectId),
                "从素材集 " + collectionId + " 导入 " + boundCount + " 个素材");

        for (Long mid : newlyBoundMaterialIds) {
            traceabilitySvc.updateMaterialCounters(mid);
            traceabilitySvc.logTrail(mid, principal, "IMPORT_FROM_COLLECTION", "PROJECT_USAGE", "PROJECT", String.valueOf(projectId), project.getName(), null, null);
        }

        return Map.of("projectId", projectId, "collectionId", collectionId, "boundCount", boundCount);
    }

    private Project mustProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    private void ensureCanView(UserPrincipal principal, Long projectId, Project project) {
        if ("ADMIN".equals(principal.getRole()) || Objects.equals(project.getOwnerId(), principal.getId())) {
            return;
        }

        ProjectCollaborator collaborator = projectCollaboratorMapper.selectOne(new LambdaQueryWrapper<ProjectCollaborator>()
                .eq(ProjectCollaborator::getProjectId, projectId)
                .eq(ProjectCollaborator::getUserId, principal.getId())
                .last("LIMIT 1"));
        if (collaborator == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无项目访问权限");
        }
    }

    private void ensureCanEdit(UserPrincipal principal, Long projectId, Project project) {
        if ("ADMIN".equals(principal.getRole()) || Objects.equals(project.getOwnerId(), principal.getId())) {
            return;
        }

        ProjectCollaborator collaborator = projectCollaboratorMapper.selectOne(new LambdaQueryWrapper<ProjectCollaborator>()
                .eq(ProjectCollaborator::getProjectId, projectId)
                .eq(ProjectCollaborator::getUserId, principal.getId())
                .last("LIMIT 1"));

        if (collaborator == null || !"EDITOR".equalsIgnoreCase(collaborator.getRole())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无项目编辑权限");
        }
    }

    private boolean canAccessMaterial(UserPrincipal principal, Material material) {
        if ("ADMIN".equals(principal.getRole())) {
            return true;
        }
        if (Objects.equals(material.getOwnerId(), principal.getId())) {
            return true;
        }
        if ("PUBLIC".equalsIgnoreCase(material.getVisibility())) {
            return true;
        }
        if ("TEAM".equalsIgnoreCase(material.getVisibility()) && principal.getTeamId() != null) {
            User owner = userMapper.selectById(material.getOwnerId());
            return owner != null && Objects.equals(owner.getTeamId(), principal.getTeamId());
        }
        return false;
    }

    private Map<String, Object> projectToMap(Project project) {
        List<ProjectMaterialRel> rels = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>()
                .eq(ProjectMaterialRel::getProjectId, project.getId()));
        List<Long> materialIds = rels.stream().map(ProjectMaterialRel::getMaterialId).toList();

        List<ProjectCollaborator> collaborators = projectCollaboratorMapper.selectList(new LambdaQueryWrapper<ProjectCollaborator>()
                .eq(ProjectCollaborator::getProjectId, project.getId()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", project.getId());
        result.put("name", project.getName());
        result.put("description", project.getDescription() == null ? "" : project.getDescription());
        result.put("ownerId", project.getOwnerId());
        result.put("status", project.getStatus());
        result.put("teamId", project.getTeamId());
        result.put("exportFormat", project.getExportFormat());
        result.put("currentVersionId", project.getCurrentVersionId());
        result.put("materials", materialIds);
        result.put("collaborators", collaborators.stream()
                .map(item -> Map.of("userId", item.getUserId(), "role", item.getRole()))
                .toList());
        result.put("updatedAt", project.getUpdatedAt());
        result.put("createdAt", project.getCreatedAt());
        return result;
    }
}
