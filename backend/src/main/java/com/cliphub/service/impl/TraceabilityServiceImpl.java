package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.CleanupUnusedRequest;
import com.cliphub.dto.CopyProjectRequest;
import com.cliphub.dto.DeleteImpactRequest;
import com.cliphub.dto.MaterialSourceRequest;
import com.cliphub.dto.MaterialTransferRequest;
import com.cliphub.entity.*;
import com.cliphub.entity.Collection;
import com.cliphub.mapper.*;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import com.cliphub.service.TraceabilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceabilityServiceImpl implements TraceabilityService {

    private final MaterialUsageTrailMapper trailMapper;
    private final MaterialModificationHistoryMapper modHistoryMapper;
    private final ProjectVersionMaterialSnapshotMapper snapshotMapper;
    private final MaterialTransferRecordMapper transferRecordMapper;
    private final AdminCleanupBatchMapper cleanupBatchMapper;
    private final MaterialMapper materialMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMaterialRelMapper projectMaterialRelMapper;
    private final ProjectVersionMapper projectVersionMapper;
    private final CollectionMapper collectionMapper;
    private final CollectionMaterialRelMapper collectionMaterialRelMapper;
    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final MaterialTagRelMapper materialTagRelMapper;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final int RECYCLE_DAYS = 30;

    @Override
    public Map<String, Object> getMaterialUsageTimeline(UserPrincipal principal, Long materialId) {
        Material material = getAccessibleMaterial(principal, materialId);
        List<Map<String, Object>> timeline = new ArrayList<>();

        timeline.add(buildTimelineItem(
                material.getCreatedAt(), "UPLOAD", "LIFECYCLE",
                "素材上传", null, null,
                Map.of("uploaderId", material.getOwnerId(),
                        "uploaderName", getUserName(material.getOwnerId()))
        ));

        for (MaterialUsageTrail trail : trailMapper.selectList(
                new LambdaQueryWrapper<MaterialUsageTrail>()
                        .eq(MaterialUsageTrail::getMaterialId, materialId)
                        .orderByDesc(MaterialUsageTrail::getCreatedAt))) {
            timeline.add(buildTimelineItemFromTrail(trail));
        }

        for (MaterialModificationHistory h : modHistoryMapper.selectList(
                new LambdaQueryWrapper<MaterialModificationHistory>()
                        .eq(MaterialModificationHistory::getMaterialId, materialId)
                        .orderByDesc(MaterialModificationHistory::getCreatedAt))) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("fieldName", h.getFieldName());
            detail.put("oldValue", h.getOldValue());
            detail.put("newValue", h.getNewValue());
            timeline.add(buildTimelineItem(
                    h.getCreatedAt(), "MODIFY", "MODIFICATION",
                    getFieldLabel(h.getFieldName()) + " 变更",
                    null, null,
                    Map.of("userId", h.getUserId(),
                            "username", h.getUsername() != null ? h.getUsername() : getUserName(h.getUserId()),
                            "changes", detail)
            ));
        }

        timeline.sort((a, b) -> ((LocalDateTime) b.get("timestamp")).compareTo((LocalDateTime) a.get("timestamp")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialId", materialId);
        result.put("materialTitle", material.getTitle());
        result.put("totalEvents", timeline.size());
        result.put("timeline", timeline);
        return result;
    }

    @Override
    public Map<String, Object> getMaterialReferenceStats(UserPrincipal principal, Long materialId) {
        getAccessibleMaterial(principal, materialId);
        long pc = projectMaterialRelMapper.selectCount(new LambdaQueryWrapper<ProjectMaterialRel>()
                .eq(ProjectMaterialRel::getMaterialId, materialId));
        long cc = collectionMaterialRelMapper.selectCount(new LambdaQueryWrapper<CollectionMaterialRel>()
                .eq(CollectionMaterialRel::getMaterialId, materialId));
        long fc = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getMaterialId, materialId));
        long dc = getDownloadCount(materialId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialId", materialId);
        result.put("projectUsageCount", pc);
        result.put("collectionUsageCount", cc);
        result.put("favoriteCount", fc);
        result.put("downloadCount", dc);
        result.put("totalReferences", pc + cc + fc);
        result.put("canSafelyDelete", pc == 0 && cc == 0);
        return result;
    }

    @Override
    public List<Map<String, Object>> getMaterialProjects(UserPrincipal principal, Long materialId) {
        Material material = getAccessibleMaterial(principal, materialId);
        List<ProjectMaterialRel> rels = projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getMaterialId, materialId));
        if (rels.isEmpty()) return List.of();

        Set<Long> projectIds = rels.stream().map(ProjectMaterialRel::getProjectId).collect(Collectors.toSet());
        Map<Long, ProjectMaterialRel> relMap = rels.stream()
                .collect(Collectors.toMap(ProjectMaterialRel::getProjectId, r -> r));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Project project : projectMapper.selectBatchIds(projectIds)) {
            if (!canAccessProjectLight(principal, project)) continue;
            ProjectMaterialRel rel = relMap.get(project.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("projectId", project.getId());
            item.put("projectName", project.getName());
            item.put("projectStatus", project.getStatus());
            item.put("ownerId", project.getOwnerId());
            item.put("ownerName", getUserName(project.getOwnerId()));
            item.put("addedBy", rel.getAddedBy());
            item.put("addedByName", getUserName(rel.getAddedBy()));
            item.put("addedAt", rel.getAddedAt() != null ? rel.getAddedAt() : rel.getCreatedAt());
            item.put("sourceType", rel.getSourceType());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMaterialCollections(UserPrincipal principal, Long materialId) {
        getAccessibleMaterial(principal, materialId);
        List<CollectionMaterialRel> rels = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>().eq(CollectionMaterialRel::getMaterialId, materialId));
        if (rels.isEmpty()) return List.of();

        Set<Long> colIds = rels.stream().map(CollectionMaterialRel::getCollectionId).collect(Collectors.toSet());
        Map<Long, CollectionMaterialRel> relMap = rels.stream()
                .collect(Collectors.toMap(CollectionMaterialRel::getCollectionId, r -> r));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Collection col : collectionMapper.selectBatchIds(colIds)) {
            if (!canAccessCollection(principal, col)) continue;
            CollectionMaterialRel rel = relMap.get(col.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("collectionId", col.getId());
            item.put("collectionName", col.getName());
            item.put("visibility", col.getVisibility());
            item.put("ownerId", col.getOwnerId());
            item.put("ownerName", getUserName(col.getOwnerId()));
            item.put("addedBy", rel.getAddedBy());
            item.put("addedByName", getUserName(rel.getAddedBy()));
            item.put("addedAt", rel.getAddedAt() != null ? rel.getAddedAt() : rel.getCreatedAt());
            item.put("note", rel.getNote());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMaterialModificationHistory(UserPrincipal principal, Long materialId) {
        getAccessibleMaterial(principal, materialId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MaterialModificationHistory h : modHistoryMapper.selectList(
                new LambdaQueryWrapper<MaterialModificationHistory>()
                        .eq(MaterialModificationHistory::getMaterialId, materialId)
                        .orderByDesc(MaterialModificationHistory::getCreatedAt))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", h.getId());
            item.put("fieldName", h.getFieldName());
            item.put("fieldLabel", getFieldLabel(h.getFieldName()));
            item.put("oldValue", h.getOldValue());
            item.put("newValue", h.getNewValue());
            item.put("userId", h.getUserId());
            item.put("username", h.getUsername() != null ? h.getUsername() : getUserName(h.getUserId()));
            item.put("createdAt", h.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getProjectMaterialInventory(UserPrincipal principal, Long projectId) {
        Project project = getAccessibleProject(principal, projectId);
        List<ProjectMaterialRel> rels = projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getProjectId, projectId));

        List<Map<String, Object>> materials = new ArrayList<>();
        List<Map<String, Object>> deletedMaterials = new ArrayList<>();
        Set<Long> ownerIds = new HashSet<>();
        Map<String, Integer> sourceStats = new HashMap<>();

        for (ProjectMaterialRel rel : rels) {
            Material material = materialMapper.selectById(rel.getMaterialId());
            Map<String, Object> item = buildProjectMaterialItem(rel, material, project);
            if (material == null || (material.getIsDeleted() != null && material.getIsDeleted() == 1)) {
                deletedMaterials.add(item);
            } else {
                materials.add(item);
            }
            if (rel.getAddedBy() != null) ownerIds.add(rel.getAddedBy());
            if (rel.getMaterialOwnerId() != null) ownerIds.add(rel.getMaterialOwnerId());
            String st = rel.getSourceType() != null ? rel.getSourceType() : "DIRECT_BIND";
            sourceStats.merge(st, 1, Integer::sum);
        }

        List<Map<String, Object>> contributors = new ArrayList<>();
        for (Long uid : ownerIds) {
            User u = userMapper.selectById(uid);
            if (u == null) continue;
            long count = materials.stream().filter(m -> Objects.equals(m.get("contributorId"), uid)).count();
            contributors.add(Map.of(
                    "userId", uid, "username", u.getUsername(),
                    "displayName", u.getDisplayName(), "materialCount", count
            ));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", project.getName());
        result.put("totalCount", rels.size());
        result.put("activeCount", materials.size());
        result.put("deletedCount", deletedMaterials.size());
        result.put("materials", materials);
        result.put("deletedMaterials", deletedMaterials);
        result.put("contributors", contributors);
        result.put("sourceBreakdown", sourceStats);
        return result;
    }

    @Override
    public List<Map<String, Object>> getProjectMaterialContributors(UserPrincipal principal, Long projectId) {
        getAccessibleProject(principal, projectId);
        List<ProjectMaterialRel> rels = projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getProjectId, projectId));
        Map<Long, List<ProjectMaterialRel>> byContributor = new HashMap<>();
        for (ProjectMaterialRel rel : rels) {
            Long cid = rel.getAddedBy() != null ? rel.getAddedBy() : rel.getMaterialOwnerId();
            if (cid == null) continue;
            byContributor.computeIfAbsent(cid, k -> new ArrayList<>()).add(rel);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<ProjectMaterialRel>> entry : byContributor.entrySet()) {
            User u = userMapper.selectById(entry.getKey());
            if (u == null) continue;
            long own = entry.getValue().stream().filter(r -> Objects.equals(r.getMaterialOwnerId(), entry.getKey())).count();
            result.add(Map.of(
                    "userId", entry.getKey(), "username", u.getUsername(),
                    "displayName", u.getDisplayName(), "avatarUrl", nullToEmpty(u.getAvatarUrl()),
                    "totalContributed", (long) entry.getValue().size(),
                    "ownMaterials", own, "sharedMaterials", (long) entry.getValue().size() - own,
                    "contributionPercent", rels.isEmpty() ? 0 : Math.round(entry.getValue().size() * 100.0 / rels.size())
            ));
        }
        result.sort((a, b) -> Long.compare((Long) ((Map) b).get("totalContributed"), (Long) ((Map) a).get("totalContributed")));
        return result;
    }

    @Override
    public List<Map<String, Object>> checkProjectMaterialAvailability(UserPrincipal principal, Long projectId) {
        getAccessibleProject(principal, projectId);
        List<Map<String, Object>> issues = new ArrayList<>();
        for (ProjectMaterialRel rel : projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getProjectId, projectId))) {
            Material m = materialMapper.selectById(rel.getMaterialId());
            Map<String, Object> issue = checkMaterialAvailability(m, rel, principal);
            if (issue != null) issues.add(issue);
        }
        return issues;
    }

    @Override
    public Map<String, Object> exportProjectMaterialInventory(UserPrincipal principal, Long projectId, String format) {
        Map<String, Object> inv = getProjectMaterialInventory(principal, projectId);
        List<Map<String, Object>> materials = (List<Map<String, Object>>) inv.get("materials");
        List<Map<String, Object>> deleted = (List<Map<String, Object>>) inv.get("deletedMaterials");
        List<Map<String, Object>> all = new ArrayList<>();
        all.addAll(materials);
        all.addAll(deleted);

        StringBuilder csv = new StringBuilder();
        csv.append("素材ID,标题,类型,分类,上传者,贡献者,来源,状态,大小,格式,添加时间,可见性\n");
        for (Map<String, Object> m : all) {
            csv.append(csvStr(m, "materialId")).append(",").append(csvStr(m, "title")).append(",")
                    .append(csvStr(m, "type")).append(",").append(csvStr(m, "categoryName")).append(",")
                    .append(csvStr(m, "ownerName")).append(",").append(csvStr(m, "contributorName")).append(",")
                    .append(csvStr(m, "sourceType")).append(",").append(csvStr(m, "status")).append(",")
                    .append(csvStr(m, "sizeBytes")).append(",").append(csvStr(m, "format")).append(",")
                    .append(csvStr(m, "addedAt")).append(",").append(csvStr(m, "visibility")).append("\n");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("projectId", projectId);
        r.put("format", "csv");
        r.put("totalMaterials", all.size());
        r.put("content", csv.toString());
        r.put("filename", "project-" + projectId + "-materials.csv");
        return r;
    }

    @Override
    public List<Map<String, Object>> getHotMaterialsRanking(UserPrincipal principal, String dimension, int limit) {
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().eq(Material::getIsDeleted, 0).last("LIMIT 500"));
        Map<Long, Long> pMap = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream().collect(Collectors.groupingBy(ProjectMaterialRel::getMaterialId, Collectors.counting()));
        Map<Long, Long> cMap = collectionMaterialRelMapper.selectList(new LambdaQueryWrapper<CollectionMaterialRel>())
                .stream().collect(Collectors.groupingBy(CollectionMaterialRel::getMaterialId, Collectors.counting()));
        Map<Long, Long> fMap = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>())
                .stream().collect(Collectors.groupingBy(Favorite::getMaterialId, Collectors.counting()));

        String dim = dimension != null ? dimension.toUpperCase() : "COMPOSITE";
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Material m : materials) {
            if (!canSeeMaterialInStats(principal, m)) continue;
            long d = nullSafe(m.getDownloadCount());
            long fv = nullSafe(m.getFavoriteCount());
            long s = nullSafe(m.getShareCount());
            long p = pMap.getOrDefault(m.getId(), 0L);
            long c = cMap.getOrDefault(m.getId(), 0L);
            long composite = d * 3 + fv * 4 + s * 2 + p * 5 + c;
            long sort = switch (dim) {
                case "DOWNLOAD" -> d; case "FAVORITE" -> fv; case "PROJECT" -> p;
                case "COLLECTION" -> c; case "SHARE" -> s; default -> composite;
            };
            Map<String, Object> scoreMap = new LinkedHashMap<>();
            scoreMap.put("materialId", m.getId());
            scoreMap.put("title", m.getTitle());
            scoreMap.put("type", m.getType());
            scoreMap.put("downloads", d);
            scoreMap.put("favorites", fv);
            scoreMap.put("shares", s);
            scoreMap.put("projectUses", p);
            scoreMap.put("collectionUses", c);
            scoreMap.put("score", sort);
            scoreMap.put("compositeScore", composite);
            scoreMap.put("ownerId", m.getOwnerId());
            scoreMap.put("ownerName", getUserName(m.getOwnerId()));
            scored.add(scoreMap);
        }
        scored.sort((a, b) -> Long.compare((Long) ((Map) b).get("score"), (Long) ((Map) a).get("score")));
        return scored.stream().limit(Math.max(1, limit)).toList();
    }

    @Override
    public List<Map<String, Object>> getUnusedMaterials(UserPrincipal principal) {
        LambdaQueryWrapper<Material> w = new LambdaQueryWrapper<Material>().eq(Material::getIsDeleted, 0);
        if (!"ADMIN".equals(principal.getRole())) w.eq(Material::getOwnerId, principal.getId());
        List<Material> materials = materialMapper.selectList(w);

        Set<Long> usedP = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream().map(ProjectMaterialRel::getMaterialId).collect(Collectors.toSet());
        Set<Long> usedC = collectionMaterialRelMapper.selectList(new LambdaQueryWrapper<CollectionMaterialRel>())
                .stream().map(CollectionMaterialRel::getMaterialId).collect(Collectors.toSet());

        List<Map<String, Object>> unused = new ArrayList<>();
        for (Material m : materials) {
            if (usedP.contains(m.getId()) || usedC.contains(m.getId())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", m.getId());
            item.put("title", m.getTitle());
            item.put("type", m.getType());
            item.put("sizeBytes", m.getSizeBytes());
            item.put("createdAt", m.getCreatedAt());
            item.put("downloadCount", m.getDownloadCount());
            item.put("daysSinceUpload", daysBetween(m.getCreatedAt(), LocalDateTime.now()));
            unused.add(item);
        }
        unused.sort((a, b) -> Long.compare((Long) ((Map) b).get("daysSinceUpload"), (Long) ((Map) a).get("daysSinceUpload")));
        return unused;
    }

    @Override
    public List<Map<String, Object>> getHighReuseMaterials(UserPrincipal principal, int threshold) {
        Map<Long, Long> pMap = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream().collect(Collectors.groupingBy(ProjectMaterialRel::getMaterialId, Collectors.counting()));
        int min = Math.max(2, threshold);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : pMap.entrySet()) {
            if (e.getValue() < min) continue;
            Material m = materialMapper.selectById(e.getKey());
            if (m == null || (m.getIsDeleted() != null && m.getIsDeleted() == 1)) continue;
            if (!canSeeMaterialInStats(principal, m)) continue;
            result.add(Map.of(
                    "materialId", m.getId(), "title", m.getTitle(), "type", m.getType(),
                    "projectUseCount", e.getValue(), "isTeamFeatured", e.getValue() >= 5,
                    "ownerId", m.getOwnerId(), "ownerName", getUserName(m.getOwnerId())
            ));
        }
        result.sort((a, b) -> Long.compare((Long) ((Map) b).get("projectUseCount"), (Long) ((Map) a).get("projectUseCount")));
        return result;
    }

    @Override
    public List<Map<String, Object>> getTeamContributionStats(UserPrincipal principal) {
        List<Material> all = materialMapper.selectList(new LambdaQueryWrapper<Material>().eq(Material::getIsDeleted, 0));
        Map<Long, Long> pMap = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream().collect(Collectors.groupingBy(ProjectMaterialRel::getMaterialId, Collectors.counting()));

        Map<Long, long[]> byUser = new HashMap<>();
        for (Material m : all) {
            if (principal.getTeamId() != null) {
                User owner = userMapper.selectById(m.getOwnerId());
                if (owner == null || !Objects.equals(owner.getTeamId(), principal.getTeamId())) continue;
            }
            byUser.computeIfAbsent(m.getOwnerId(), k -> new long[4]);
            long[] s = byUser.get(m.getOwnerId());
            s[0]++; s[1] += nullSafe(m.getDownloadCount());
            s[2] += nullSafe(m.getFavoriteCount()); s[3] += pMap.getOrDefault(m.getId(), 0L);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, long[]> e : byUser.entrySet()) {
            User u = userMapper.selectById(e.getKey());
            if (u == null) continue;
            long[] s = e.getValue();
            long score = s[0] * 2 + s[1] + s[2] * 3 + s[3] * 5;
            result.add(Map.of(
                    "userId", u.getId(), "username", u.getUsername(),
                    "displayName", u.getDisplayName(), "avatarUrl", nullToEmpty(u.getAvatarUrl()),
                    "uploadCount", s[0], "downloadTotal", s[1],
                    "favoriteTotal", s[2], "projectUseTotal", s[3], "contributionScore", score
            ));
        }
        result.sort((a, b) -> Long.compare((Long) ((Map) b).get("contributionScore"), (Long) ((Map) a).get("contributionScore")));
        return result;
    }

    @Override
    public List<Map<String, Object>> recommendMaterialsForProject(UserPrincipal principal, Long projectId, int limit) {
        Project project = getAccessibleProject(principal, projectId);
        List<ProjectMaterialRel> cur = projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getProjectId, projectId));
        Set<Long> currentIds = cur.stream().map(ProjectMaterialRel::getMaterialId).collect(Collectors.toSet());
        Set<Long> categoryIds = cur.stream()
                .map(r -> materialMapper.selectById(r.getMaterialId()))
                .filter(Objects::nonNull).map(Material::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> ownerIds = cur.stream().map(ProjectMaterialRel::getMaterialOwnerId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<Material> candidates = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().eq(Material::getIsDeleted, 0)
                        .orderByDesc(Material::getCreatedAt).last("LIMIT 500"));
        Map<Long, Long> pMap = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream().collect(Collectors.groupingBy(ProjectMaterialRel::getMaterialId, Collectors.counting()));

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Material m : candidates) {
            if (currentIds.contains(m.getId())) continue;
            if (!canAccessMaterial(principal, m)) continue;
            long score = 0;
            if (categoryIds.contains(m.getCategoryId())) score += 30;
            if (ownerIds.contains(m.getOwnerId())) score += 20;
            score += pMap.getOrDefault(m.getId(), 0L) * 5;
            score += nullSafe(m.getFavoriteCount()) * 2;
            score += nullSafe(m.getDownloadCount());
            scored.add(Map.of(
                    "materialId", m.getId(), "title", m.getTitle(), "type", m.getType(),
                    "score", score, "matchCategory", categoryIds.contains(m.getCategoryId()),
                    "matchOwner", ownerIds.contains(m.getOwnerId()),
                    "projectUses", pMap.getOrDefault(m.getId(), 0L),
                    "previewUrl", "/api/materials/" + m.getId() + "/preview"
            ));
        }
        scored.sort((a, b) -> Long.compare((Long) ((Map) b).get("score"), (Long) ((Map) a).get("score")));
        return scored.stream().limit(Math.max(1, limit)).toList();
    }

    @Override
    public Map<String, Object> assessDeleteImpact(UserPrincipal principal, Long materialId) {
        Material material = getEditableMaterial(principal, materialId);
        List<ProjectMaterialRel> pr = projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getMaterialId, materialId));
        List<CollectionMaterialRel> cr = collectionMaterialRelMapper.selectList(
                new LambdaQueryWrapper<CollectionMaterialRel>().eq(CollectionMaterialRel::getMaterialId, materialId));
        Long fc = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>().eq(Favorite::getMaterialId, materialId));

        Set<Long> po = new HashSet<>(), co = new HashSet<>();
        List<Map<String, Object>> ap = new ArrayList<>(), ac = new ArrayList<>();
        for (ProjectMaterialRel r : pr) {
            Project p = projectMapper.selectById(r.getProjectId());
            if (p != null) { po.add(p.getOwnerId()); ap.add(Map.of(
                    "projectId", p.getId(), "projectName", p.getName(),
                    "ownerId", p.getOwnerId(), "ownerName", getUserName(p.getOwnerId()))); }
        }
        for (CollectionMaterialRel r : cr) {
            Collection c = collectionMapper.selectById(r.getCollectionId());
            if (c != null) { co.add(c.getOwnerId()); ac.add(Map.of(
                    "collectionId", c.getId(), "collectionName", c.getName(),
                    "ownerId", c.getOwnerId(), "ownerName", getUserName(c.getOwnerId()))); }
        }
        Set<Long> affected = new HashSet<>();
        affected.addAll(po); affected.addAll(co); affected.remove(principal.getId());

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("materialId", materialId); r.put("materialTitle", material.getTitle());
        r.put("projectCount", (long) pr.size()); r.put("collectionCount", (long) cr.size());
        r.put("favoriteCount", fc);
        r.put("totalImpact", (long) pr.size() + cr.size() + fc);
        r.put("canSafelyDelete", pr.isEmpty() && cr.isEmpty());
        r.put("affectedProjects", ap); r.put("affectedCollections", ac);
        r.put("affectedUserCount", affected.size()); r.put("affectedUserIds", affected);
        r.put("recycleDays", RECYCLE_DAYS);
        r.put("availableStrategies", List.of(
                Map.of("strategy", "RECYCLE", "label", "移入回收站(30天可恢复)", "recommended", true),
                Map.of("strategy", "NOTIFY_DELETE", "label", "通知用户后移入回收站"),
                Map.of("strategy", "FORCE_DELETE", "label", "强制删除并清理引用(不可恢复)")
        ));
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> deleteMaterialWithStrategy(UserPrincipal principal, Long materialId, DeleteImpactRequest request) {
        Material material = getEditableMaterial(principal, materialId);
        String strategy = request != null ? request.getStrategy() : "RECYCLE";
        long pc = projectMaterialRelMapper.selectCount(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getMaterialId, materialId));
        long cc = collectionMaterialRelMapper.selectCount(
                new LambdaQueryWrapper<CollectionMaterialRel>().eq(CollectionMaterialRel::getMaterialId, materialId));

        if ("FORCE_DELETE".equalsIgnoreCase(strategy)) {
            forceDeleteMaterial(materialId, material, principal);
            auditLogService.log(principal, "FORCE_DELETE_MATERIAL", "MATERIAL", String.valueOf(materialId),
                    "强制删除素材,清理" + pc + "个项目引用," + cc + "个素材集引用");
        } else {
            softDeleteToRecycle(materialId, material, principal);
            if (request != null && Boolean.TRUE.equals(request.isNotifyUsers())) {
                logTrail(materialId, principal, "DELETE_NOTIFY", "LIFECYCLE", null, null, "已通知受影响用户", null, null);
            }
            auditLogService.log(principal, "RECYCLE_MATERIAL", "MATERIAL", String.valueOf(materialId), "移入回收站");
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("materialId", materialId); r.put("strategy", strategy);
        r.put("projectReferencesCleaned", pc); r.put("collectionReferencesCleaned", cc);
        r.put("recoverable", !"FORCE_DELETE".equalsIgnoreCase(strategy));
        r.put("recycleExpireAt", !"FORCE_DELETE".equalsIgnoreCase(strategy) ? LocalDateTime.now().plusDays(RECYCLE_DAYS) : null);
        return r;
    }

    @Override
    public List<Map<String, Object>> listRecycleBin(UserPrincipal principal) {
        LambdaQueryWrapper<Material> w = new LambdaQueryWrapper<Material>()
                .eq(Material::getIsDeleted, 1).orderByDesc(Material::getDeletedAt);
        if (!"ADMIN".equals(principal.getRole())) w.eq(Material::getOwnerId, principal.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Material m : materialMapper.selectList(w)) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("materialId", m.getId());
            it.put("title", m.getOriginalTitle() != null ? m.getOriginalTitle() : m.getTitle());
            it.put("type", m.getType()); it.put("sizeBytes", m.getSizeBytes());
            it.put("deletedBy", m.getDeletedBy()); it.put("deletedByName", getUserName(m.getDeletedBy()));
            it.put("deletedAt", m.getDeletedAt()); it.put("recycleExpireAt", m.getRecycleExpireAt());
            it.put("daysRemaining", daysRemaining(m.getRecycleExpireAt()));
            it.put("canRestore", daysRemaining(m.getRecycleExpireAt()) > 0);
            result.add(it);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> restoreFromRecycleBin(UserPrincipal principal, Long materialId) {
        Material m = materialMapper.selectById(materialId);
        if (m == null) throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        if (!"ADMIN".equals(principal.getRole()) && !Objects.equals(m.getOwnerId(), principal.getId()))
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权恢复");
        if (m.getIsDeleted() == null || m.getIsDeleted() == 0) throw new BusinessException("素材不在回收站");
        if (daysRemaining(m.getRecycleExpireAt()) <= 0) throw new BusinessException("保留期已过");

        m.setIsDeleted(0); m.setDeletedBy(null); m.setDeletedAt(null);
        m.setRecycleExpireAt(null); m.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(m);
        projectMaterialRelMapper.update(null, new LambdaUpdateWrapper<ProjectMaterialRel>()
                .set(ProjectMaterialRel::getMaterialStatus, "ACTIVE")
                .eq(ProjectMaterialRel::getMaterialId, materialId));

        logTrail(materialId, principal, "RESTORE", "LIFECYCLE", null, null, "从回收站恢复", null, null);
        auditLogService.log(principal, "RESTORE_MATERIAL", "MATERIAL", String.valueOf(materialId), "恢复素材");

        return Map.of("materialId", materialId, "restored", true,
                "projectReferencesRestored", projectMaterialRelMapper.selectCount(
                        new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getMaterialId, materialId)));
    }

    @Override
    @Transactional
    public Map<String, Object> permanentlyDeleteFromRecycleBin(UserPrincipal principal, Long materialId) {
        Material m = materialMapper.selectById(materialId);
        if (m == null) throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        if (!"ADMIN".equals(principal.getRole()) && !Objects.equals(m.getOwnerId(), principal.getId()))
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权操作");
        forceDeleteMaterial(materialId, m, principal);
        auditLogService.log(principal, "PERMANENT_DELETE", "MATERIAL", String.valueOf(materialId), "永久删除");
        return Map.of("materialId", materialId, "permanentlyDeleted", true);
    }

    @Override
    @Transactional
    public Map<String, Object> setMaterialSourceInfo(UserPrincipal principal, Long materialId, MaterialSourceRequest request) {
        Material m = getEditableMaterial(principal, materialId);
        if (request.getSourceType() != null) m.setSourceType(request.getSourceType());
        if (request.getSourceNote() != null) m.setSourceNote(request.getSourceNote());
        if (request.getOwnershipType() != null) m.setOwnershipType(request.getOwnershipType());
        if (request.getDepartmentId() != null) m.setDepartmentId(request.getDepartmentId());
        m.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(m);
        logTrail(materialId, principal, "UPDATE_SOURCE", "METADATA", null, null, "更新来源与归属", null, null);
        return Map.of("materialId", materialId, "sourceType", m.getSourceType(), "ownershipType", m.getOwnershipType());
    }

    @Override
    @Transactional
    public Map<String, Object> transferMaterialsOwnership(UserPrincipal principal, MaterialTransferRequest request) {
        if (request.getMaterialIds() == null || request.getMaterialIds().isEmpty())
            throw new BusinessException("请指定素材");
        if (request.getToUserId() == null) throw new BusinessException("请指定接收用户");
        User to = userMapper.selectById(request.getToUserId());
        if (to == null) throw new BusinessException("接收用户不存在");
        boolean admin = "ADMIN".equals(principal.getRole());

        int transferred = 0;
        List<Long> failed = new ArrayList<>();
        for (Long mid : request.getMaterialIds()) {
            Material m = materialMapper.selectById(mid);
            if (m == null || (!admin && !Objects.equals(m.getOwnerId(), principal.getId()))) {
                failed.add(mid); continue;
            }
            Long fromId = m.getOwnerId();
            m.setOwnerId(request.getToUserId()); m.setUpdatedAt(LocalDateTime.now());
            materialMapper.updateById(m);
            projectMaterialRelMapper.update(null, new LambdaUpdateWrapper<ProjectMaterialRel>()
                    .set(ProjectMaterialRel::getMaterialOwnerId, request.getToUserId())
                    .eq(ProjectMaterialRel::getMaterialId, mid));

            MaterialTransferRecord tr = new MaterialTransferRecord();
            tr.setMaterialId(mid); tr.setFromUserId(fromId); tr.setToUserId(request.getToUserId());
            tr.setTransferNote(request.getTransferNote()); tr.setOperatorId(principal.getId());
            tr.setCreatedAt(LocalDateTime.now());
            transferRecordMapper.insert(tr);

            logTrail(mid, principal, "OWNERSHIP_TRANSFER", "OWNERSHIP",
                    "USER", String.valueOf(request.getToUserId()), to.getDisplayName(),
                    String.valueOf(fromId), String.valueOf(request.getToUserId()));
            transferred++;
        }
        auditLogService.log(principal, "TRANSFER_OWNERSHIP", "MATERIAL",
                String.valueOf(request.getToUserId()), "转移" + transferred + "个素材");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("transferredCount", transferred); r.put("failedCount", failed.size());
        r.put("failedIds", failed); r.put("toUserId", request.getToUserId());
        r.put("toUserName", to.getDisplayName());
        return r;
    }

    @Override
    public List<Map<String, Object>> getTransferHistory(UserPrincipal principal, Long materialId) {
        getAccessibleMaterial(principal, materialId);
        List<Map<String, Object>> r = new ArrayList<>();
        for (MaterialTransferRecord tr : transferRecordMapper.selectList(
                new LambdaQueryWrapper<MaterialTransferRecord>().eq(MaterialTransferRecord::getMaterialId, materialId)
                        .orderByDesc(MaterialTransferRecord::getCreatedAt))) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("id", tr.getId()); it.put("fromUserId", tr.getFromUserId());
            it.put("fromUserName", getUserName(tr.getFromUserId()));
            it.put("toUserId", tr.getToUserId()); it.put("toUserName", getUserName(tr.getToUserId()));
            it.put("operatorId", tr.getOperatorId()); it.put("operatorName", getUserName(tr.getOperatorId()));
            it.put("transferNote", tr.getTransferNote()); it.put("createdAt", tr.getCreatedAt());
            r.add(it);
        }
        return r;
    }

    @Override
    @Transactional
    public void saveVersionMaterialSnapshot(Long versionId, Long projectId) {
        ProjectVersion v = projectVersionMapper.selectById(versionId);
        if (v == null) return;
        for (ProjectMaterialRel rel : projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getProjectId, projectId))) {
            Material m = materialMapper.selectById(rel.getMaterialId());
            if (m == null) continue;
            ProjectVersionMaterialSnapshot s = new ProjectVersionMaterialSnapshot();
            s.setVersionId(versionId); s.setProjectId(projectId);
            s.setMaterialId(m.getId()); s.setMaterialTitle(m.getTitle());
            s.setMaterialOwnerId(m.getOwnerId()); s.setMaterialVisibility(m.getVisibility());
            try { s.setMaterialSnapshot(objectMapper.writeValueAsString(m)); } catch (Exception ignored) {}
            s.setCreatedAt(LocalDateTime.now());
            snapshotMapper.insert(s);
        }
    }

    @Override
    public Map<String, Object> checkVersionMaterialsAvailability(UserPrincipal principal, Long projectId, Long versionId) {
        getAccessibleProject(principal, projectId);
        List<ProjectVersionMaterialSnapshot> snaps = snapshotMapper.selectList(
                new LambdaQueryWrapper<ProjectVersionMaterialSnapshot>()
                        .eq(ProjectVersionMaterialSnapshot::getVersionId, versionId)
                        .eq(ProjectVersionMaterialSnapshot::getProjectId, projectId));
        List<Map<String, Object>> missing = new ArrayList<>(), available = new ArrayList<>(), changed = new ArrayList<>();
        for (ProjectVersionMaterialSnapshot s : snaps) {
            Material cur = materialMapper.selectById(s.getMaterialId());
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("materialId", s.getMaterialId()); info.put("materialTitle", s.getMaterialTitle());
            info.put("snapshotVisibility", s.getMaterialVisibility());
            info.put("snapshotOwnerId", s.getMaterialOwnerId());
            if (cur == null || (cur.getIsDeleted() != null && cur.getIsDeleted() == 1)) {
                info.put("issue", "MISSING"); info.put("issueDesc", "素材已删除"); missing.add(info);
            } else if (!Objects.equals(cur.getVisibility(), s.getMaterialVisibility())) {
                info.put("currentVisibility", cur.getVisibility()); info.put("issue", "VISIBILITY_CHANGED");
                info.put("issueDesc", "可见性变更"); changed.add(info);
            } else available.add(info);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("versionId", versionId); r.put("projectId", projectId);
        r.put("totalCount", snaps.size()); r.put("availableCount", available.size());
        r.put("missingCount", missing.size()); r.put("changedCount", changed.size());
        r.put("canRollback", missing.isEmpty() && changed.isEmpty());
        r.put("missingMaterials", missing); r.put("changedMaterials", changed);
        r.put("availableMaterials", available);
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> copyProject(UserPrincipal principal, Long sourceProjectId, CopyProjectRequest request) {
        Project src = getAccessibleProject(principal, sourceProjectId);
        Project np = new Project();
        np.setName(request.getNewName() != null ? request.getNewName() : src.getName() + " 副本");
        np.setDescription(src.getDescription()); np.setOwnerId(principal.getId());
        np.setStatus("DRAFT"); np.setTeamId(src.getTeamId());
        np.setExportFormat(src.getExportFormat());
        np.setCreatedAt(LocalDateTime.now()); np.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(np);

        List<ProjectMaterialRel> rels = projectMaterialRelMapper.selectList(
                new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getProjectId, sourceProjectId));
        int bc = 0;
        for (ProjectMaterialRel rel : rels) {
            Material om = materialMapper.selectById(rel.getMaterialId());
            if (om == null || (om.getIsDeleted() != null && om.getIsDeleted() == 1)) continue;
            if (!canAccessMaterial(principal, om)) continue;
            Long fid;
            if (Boolean.TRUE.equals(request.isCopyMaterials()) && Objects.equals(om.getOwnerId(), src.getOwnerId())) {
                Material cp = new Material();
                cp.setTitle(om.getTitle() + " 副本"); cp.setDescription(om.getDescription());
                cp.setType(om.getType()); cp.setCategoryId(om.getCategoryId());
                cp.setOwnerId(principal.getId()); cp.setVisibility(om.getVisibility());
                cp.setFileName(om.getFileName()); cp.setStoragePath(om.getStoragePath());
                cp.setPreviewPath(om.getPreviewPath()); cp.setMimeType(om.getMimeType());
                cp.setFormat(om.getFormat()); cp.setSizeBytes(om.getSizeBytes());
                cp.setDurationSeconds(om.getDurationSeconds()); cp.setResolution(om.getResolution());
                cp.setDownloadCount(0L); cp.setFavoriteCount(0L); cp.setShareCount(0L);
                cp.setProjectUsageCount(0L); cp.setCollectionUsageCount(0L);
                cp.setSourceType("PROJECT_COPY"); cp.setSourceNote("复制自项目 " + sourceProjectId);
                cp.setOwnershipType(om.getOwnershipType()); cp.setIsDeleted(0);
                cp.setCreatedAt(LocalDateTime.now()); cp.setUpdatedAt(LocalDateTime.now());
                materialMapper.insert(cp); fid = cp.getId();
                logTrail(cp.getId(), principal, "MATERIAL_COPY", "LIFECYCLE",
                        "PROJECT", String.valueOf(sourceProjectId), src.getName(),
                        String.valueOf(om.getId()), String.valueOf(cp.getId()));
            } else fid = om.getId();

            ProjectMaterialRel nr = new ProjectMaterialRel();
            nr.setProjectId(np.getId()); nr.setMaterialId(fid); nr.setAddedBy(principal.getId());
            nr.setAddedAt(LocalDateTime.now()); nr.setSourceType("PROJECT_COPY");
            nr.setSourceNote("从项目 " + sourceProjectId + " 复制");
            Material rf = materialMapper.selectById(fid);
            nr.setMaterialOwnerId(rf != null ? rf.getOwnerId() : null);
            nr.setMaterialStatus("ACTIVE"); nr.setCreatedAt(LocalDateTime.now());
            projectMaterialRelMapper.insert(nr);

            if (rf != null) {
                rf.setProjectUsageCount(nullSafe(rf.getProjectUsageCount()) + 1);
                materialMapper.updateById(rf);
            }
            bc++;
        }
        ProjectVersion pv = new ProjectVersion();
        pv.setProjectId(np.getId()); pv.setVersionNo(1);
        pv.setVersionName("v1-copied-from-" + sourceProjectId);
        pv.setContentJson("{\"tracks\":[],\"timeline\":[]}");
        pv.setCreatedBy(principal.getId()); pv.setIsCurrent(1);
        pv.setCreatedAt(LocalDateTime.now());
        projectVersionMapper.insert(pv);
        np.setCurrentVersionId(pv.getId()); projectMapper.updateById(np);
        saveVersionMaterialSnapshot(pv.getId(), np.getId());
        auditLogService.log(principal, "COPY_PROJECT", "PROJECT", String.valueOf(np.getId()),
                "复制项目,源=" + sourceProjectId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("newProjectId", np.getId()); r.put("newProjectName", np.getName());
        r.put("materialBoundCount", bc);
        r.put("copyMode", Boolean.TRUE.equals(request.isCopyMaterials()) ? "COPY" : "REFERENCE");
        return r;
    }

    @Override
    @Transactional
    public Map<String, Object> adminCleanupUnusedMaterials(UserPrincipal principal, CleanupUnusedRequest request) {
        if (!"ADMIN".equals(principal.getRole())) throw new BusinessException(HttpStatus.FORBIDDEN, "仅管理员");
        List<Long> target;
        if (request.getMaterialIds() != null && !request.getMaterialIds().isEmpty()) {
            target = new ArrayList<>(request.getMaterialIds());
        } else {
            target = getUnusedMaterials(principal).stream().map(m -> (Long) m.get("materialId")).toList();
        }
        Set<Long> up = projectMaterialRelMapper.selectList(new LambdaQueryWrapper<ProjectMaterialRel>())
                .stream().map(ProjectMaterialRel::getMaterialId).collect(Collectors.toSet());
        Set<Long> uc = collectionMaterialRelMapper.selectList(new LambdaQueryWrapper<CollectionMaterialRel>())
                .stream().map(CollectionMaterialRel::getMaterialId).collect(Collectors.toSet());
        List<Long> safe = new ArrayList<>(); List<Long> skip = new ArrayList<>(); long tb = 0;
        for (Long id : target) {
            if (up.contains(id) || uc.contains(id)) { skip.add(id); continue; }
            Material m = materialMapper.selectById(id);
            if (m == null) continue;
            tb += nullSafe(m.getSizeBytes()); safe.add(id);
        }
        AdminCleanupBatch batch = new AdminCleanupBatch();
        batch.setOperatorId(principal.getId()); batch.setOperatorName(principal.getUsername());
        batch.setTotalCount(safe.size()); batch.setCleanedCount(0); batch.setTotalBytes(tb);
        batch.setStatus(request.getDryRun() != null && request.getDryRun() ? "DRY_RUN" : "PROCESSING");
        try { batch.setMaterialIds(objectMapper.writeValueAsString(safe)); } catch (Exception ignored) {}
        batch.setCreatedAt(LocalDateTime.now()); cleanupBatchMapper.insert(batch);
        int cleaned = 0;
        if (request.getDryRun() == null || !request.getDryRun()) {
            for (Long id : safe) {
                Material m = materialMapper.selectById(id);
                if (m == null) continue;
                forceDeleteMaterial(id, m, principal); cleaned++;
            }
            batch.setCleanedCount(cleaned); batch.setStatus("COMPLETED");
            batch.setCompletedAt(LocalDateTime.now()); cleanupBatchMapper.updateById(batch);
        }
        auditLogService.log(principal, "ADMIN_CLEANUP", "MATERIAL", String.valueOf(batch.getId()),
                (request.getDryRun() != null && request.getDryRun() ? "[模拟]" : "") + "清理" + cleaned + "个素材");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("batchId", batch.getId());
        r.put("isDryRun", request.getDryRun() != null && request.getDryRun());
        r.put("totalCandidates", target.size()); r.put("safeToDeleteCount", safe.size());
        r.put("skippedCount", skip.size()); r.put("cleanedCount", cleaned);
        r.put("freedBytes", tb); r.put("skippedIds", skip); r.put("status", batch.getStatus());
        return r;
    }

    private Map<String, Object> buildTimelineItemFromTrail(MaterialUsageTrail t) {
        Map<String, Object> it = new LinkedHashMap<>();
        it.put("timestamp", t.getCreatedAt()); it.put("action", t.getAction());
        it.put("actionType", t.getActionType()); it.put("title", t.getDetail());
        if (t.getTargetType() != null) {
            it.put("targetType", t.getTargetType()); it.put("targetId", t.getTargetId());
            it.put("targetName", t.getTargetName());
        }
        it.put("userId", t.getUserId());
        it.put("username", t.getUsername() != null ? t.getUsername() : getUserName(t.getUserId()));
        if (t.getOldValue() != null || t.getNewValue() != null) {
            it.put("oldValue", t.getOldValue()); it.put("newValue", t.getNewValue());
        }
        return it;
    }

    private Map<String, Object> buildTimelineItem(LocalDateTime ts, String action, String at,
                                                  String title, String tt, String tid,
                                                  Map<String, Object> extra) {
        Map<String, Object> it = new LinkedHashMap<>();
        it.put("timestamp", ts); it.put("action", action); it.put("actionType", at);
        it.put("title", title);
        if (tt != null) { it.put("targetType", tt); it.put("targetId", tid); }
        if (extra != null) it.putAll(extra);
        return it;
    }

    private Map<String, Object> buildProjectMaterialItem(ProjectMaterialRel rel, Material m, Project p) {
        Map<String, Object> it = new LinkedHashMap<>();
        boolean del = m == null || (m.getIsDeleted() != null && m.getIsDeleted() == 1);
        String title = m != null ? m.getTitle() : (rel.getMaterialId() != null ? "[已删除素材#" + rel.getMaterialId() + "]" : "[未知]");
        it.put("materialId", rel.getMaterialId()); it.put("title", title);
        it.put("status", del ? "DELETED" : (rel.getMaterialStatus() != null ? rel.getMaterialStatus() : "ACTIVE"));
        it.put("statusLabel", del ? "素材已失效" : "正常");
        it.put("addedBy", rel.getAddedBy());
        it.put("contributorId", rel.getAddedBy() != null ? rel.getAddedBy() : rel.getMaterialOwnerId());
        it.put("contributorName", getUserName(rel.getAddedBy()));
        it.put("addedAt", rel.getAddedAt() != null ? rel.getAddedAt() : rel.getCreatedAt());
        it.put("sourceType", rel.getSourceType() != null ? rel.getSourceType() : "DIRECT_BIND");
        it.put("sourceNote", rel.getSourceNote());
        it.put("materialOwnerId", rel.getMaterialOwnerId());
        it.put("ownerName", getUserName(rel.getMaterialOwnerId()));
        boolean isOwn = m != null && Objects.equals(m.getOwnerId(), p.getOwnerId());
        boolean isShared = !isOwn && rel.getMaterialOwnerId() != null;
        it.put("ownership", isOwn ? "OWNER" : (isShared ? "SHARED" : "EXTERNAL"));
        it.put("ownershipLabel", isOwn ? "自有素材" : (isShared ? "团队共享" : "外部素材"));
        if (m != null) {
            it.put("type", m.getType()); it.put("format", m.getFormat());
            it.put("sizeBytes", m.getSizeBytes()); it.put("visibility", m.getVisibility());
            it.put("downloadCount", m.getDownloadCount());
            it.put("sourceType", m.getSourceType()); it.put("ownershipType", m.getOwnershipType());
            Category cat = m.getCategoryId() != null ? categoryMapper.selectById(m.getCategoryId()) : null;
            it.put("categoryId", m.getCategoryId());
            it.put("categoryName", cat != null ? cat.getName() : "未分类");
            it.put("previewUrl", del ? null : "/api/materials/" + m.getId() + "/preview");
        }
        return it;
    }

    private Map<String, Object> checkMaterialAvailability(Material m, ProjectMaterialRel rel, UserPrincipal p) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("materialId", rel.getMaterialId());
        issue.put("title", m != null ? m.getTitle() : "[未知素材]");
        if (m == null) {
            issue.put("severity", "CRITICAL"); issue.put("type", "NOT_FOUND");
            issue.put("message", "素材不存在，可能已被永久删除"); return issue;
        }
        if (m.getIsDeleted() != null && m.getIsDeleted() == 1) {
            issue.put("severity", "WARNING"); issue.put("type", "IN_RECYCLE");
            issue.put("message", "素材在回收站中，" + daysRemaining(m.getRecycleExpireAt()) + "天后永久删除");
            issue.put("recoverable", daysRemaining(m.getRecycleExpireAt()) > 0);
            issue.put("recycleExpireAt", m.getRecycleExpireAt()); return issue;
        }
        if (!canAccessMaterial(p, m)) {
            issue.put("severity", "ERROR"); issue.put("type", "NO_PERMISSION");
            issue.put("message", "素材权限变更，当前用户无法访问");
            issue.put("currentVisibility", m.getVisibility()); return issue;
        }
        return null;
    }

    private void softDeleteToRecycle(Long mid, Material m, UserPrincipal p) {
        if (m.getOriginalTitle() == null) m.setOriginalTitle(m.getTitle());
        if (m.getOriginalPreviewPath() == null) m.setOriginalPreviewPath(m.getPreviewPath());
        m.setIsDeleted(1); m.setDeletedBy(p.getId()); m.setDeletedAt(LocalDateTime.now());
        m.setRecycleExpireAt(LocalDateTime.now().plusDays(RECYCLE_DAYS));
        m.setUpdatedAt(LocalDateTime.now()); materialMapper.updateById(m);
        projectMaterialRelMapper.update(null, new LambdaUpdateWrapper<ProjectMaterialRel>()
                .set(ProjectMaterialRel::getMaterialStatus, "DELETED")
                .eq(ProjectMaterialRel::getMaterialId, mid));
    }

    private void forceDeleteMaterial(Long mid, Material m, UserPrincipal p) {
        projectMaterialRelMapper.delete(new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getMaterialId, mid));
        collectionMaterialRelMapper.delete(new LambdaQueryWrapper<CollectionMaterialRel>().eq(CollectionMaterialRel::getMaterialId, mid));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getMaterialId, mid));
        materialTagRelMapper.delete(new LambdaQueryWrapper<MaterialTagRel>().eq(MaterialTagRel::getMaterialId, mid));
        materialMapper.deleteById(mid);
        if (m.getStoragePath() != null) { try { Files.deleteIfExists(Path.of(m.getStoragePath())); } catch (IOException ignored) {} }
    }

    public void logTrail(Long mid, UserPrincipal p, String action, String at,
                         String tt, String tid, String detail, String ov, String nv) {
        MaterialUsageTrail t = new MaterialUsageTrail();
        t.setMaterialId(mid); t.setUserId(p.getId()); t.setUsername(p.getUsername());
        t.setAction(action); t.setActionType(at); t.setTargetType(tt); t.setTargetId(tid);
        t.setDetail(detail); t.setOldValue(ov); t.setNewValue(nv);
        t.setCreatedAt(LocalDateTime.now()); trailMapper.insert(t);
    }

    public void logModification(Long mid, UserPrincipal p, String field, String ov, String nv) {
        if (Objects.equals(ov, nv)) return;
        MaterialModificationHistory h = new MaterialModificationHistory();
        h.setMaterialId(mid); h.setUserId(p.getId()); h.setUsername(p.getUsername());
        h.setFieldName(field); h.setOldValue(ov); h.setNewValue(nv);
        h.setCreatedAt(LocalDateTime.now()); modHistoryMapper.insert(h);
    }

    public void updateMaterialCounters(Long mid) {
        Material m = materialMapper.selectById(mid); if (m == null) return;
        long pc = projectMaterialRelMapper.selectCount(new LambdaQueryWrapper<ProjectMaterialRel>().eq(ProjectMaterialRel::getMaterialId, mid));
        long cc = collectionMaterialRelMapper.selectCount(new LambdaQueryWrapper<CollectionMaterialRel>().eq(CollectionMaterialRel::getMaterialId, mid));
        m.setProjectUsageCount(pc); m.setCollectionUsageCount(cc); materialMapper.updateById(m);
    }

    private Material getAccessibleMaterial(UserPrincipal p, Long mid) {
        Material m = materialMapper.selectById(mid);
        if (m == null) throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        if (m.getIsDeleted() != null && m.getIsDeleted() == 1) {
            if (!"ADMIN".equals(p.getRole()) && !Objects.equals(m.getOwnerId(), p.getId()))
                throw new BusinessException(HttpStatus.FORBIDDEN, "素材已删除");
            return m;
        }
        if (!canAccessMaterial(p, m)) throw new BusinessException(HttpStatus.FORBIDDEN, "无访问权限");
        return m;
    }

    private Material getEditableMaterial(UserPrincipal p, Long mid) {
        Material m = materialMapper.selectById(mid);
        if (m == null) throw new BusinessException(HttpStatus.NOT_FOUND, "素材不存在");
        if ("ADMIN".equals(p.getRole()) || Objects.equals(p.getId(), m.getOwnerId())) return m;
        throw new BusinessException(HttpStatus.FORBIDDEN, "仅所有者或管理员");
    }

    private Project getAccessibleProject(UserPrincipal p, Long pid) {
        Project pr = projectMapper.selectById(pid);
        if (pr == null) throw new BusinessException(HttpStatus.NOT_FOUND, "项目不存在");
        if (!canAccessProject(p, pr)) throw new BusinessException(HttpStatus.FORBIDDEN, "无项目权限");
        return pr;
    }

    private boolean canAccessProject(UserPrincipal p, Project pr) {
        if ("ADMIN".equals(p.getRole()) || Objects.equals(pr.getOwnerId(), p.getId())) return true;
        return false;
    }

    private boolean canAccessProjectLight(UserPrincipal p, Project pr) {
        if ("ADMIN".equals(p.getRole()) || Objects.equals(pr.getOwnerId(), p.getId())) return true;
        return "PUBLIC".equalsIgnoreCase(pr.getStatus());
    }

    private boolean canAccessCollection(UserPrincipal p, Collection c) {
        if ("ADMIN".equals(p.getRole()) || Objects.equals(c.getOwnerId(), p.getId())) return true;
        return "PUBLIC".equalsIgnoreCase(c.getVisibility());
    }

    private boolean canAccessMaterial(UserPrincipal p, Material m) {
        if (m == null) return false;
        if ("ADMIN".equals(p.getRole())) return true;
        if (Objects.equals(m.getOwnerId(), p.getId())) return true;
        if ("PUBLIC".equalsIgnoreCase(m.getVisibility())) return true;
        if ("TEAM".equalsIgnoreCase(m.getVisibility()) && p.getTeamId() != null) {
            User o = userMapper.selectById(m.getOwnerId());
            return o != null && Objects.equals(o.getTeamId(), p.getTeamId());
        }
        return false;
    }

    private boolean canSeeMaterialInStats(UserPrincipal p, Material m) {
        if ("ADMIN".equals(p.getRole())) return true;
        if (Objects.equals(m.getOwnerId(), p.getId())) return true;
        if ("PUBLIC".equalsIgnoreCase(m.getVisibility())) return true;
        return false;
    }

    private String getUserName(Long uid) {
        if (uid == null) return "";
        User u = userMapper.selectById(uid);
        return u != null ? u.getDisplayName() : "";
    }

    private long getDownloadCount(Long mid) {
        Material m = materialMapper.selectById(mid);
        return m != null ? nullSafe(m.getDownloadCount()) : 0L;
    }

    private String getFieldLabel(String name) {
        return switch (name) {
            case "title" -> "标题"; case "description" -> "描述";
            case "categoryId" -> "分类"; case "visibility" -> "可见性";
            case "tags" -> "标签"; case "resolution" -> "分辨率";
            case "durationSeconds" -> "时长"; default -> name;
        };
    }

    private String csvStr(Map<String, Object> m, String key) {
        Object v = m.get(key); if (v == null) return "";
        return escapeCsv(String.valueOf(v));
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private long daysBetween(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) return 0;
        return ChronoUnit.DAYS.between(a, b);
    }

    private long daysRemaining(LocalDateTime expire) {
        if (expire == null) return 0;
        return ChronoUnit.DAYS.between(LocalDateTime.now(), expire);
    }

    private long nullSafe(Long v) { return v == null ? 0L : v; }
    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
