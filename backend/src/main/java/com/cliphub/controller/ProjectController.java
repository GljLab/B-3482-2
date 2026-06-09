package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.CollaboratorRequest;
import com.cliphub.dto.ProjectCreateRequest;
import com.cliphub.dto.SaveVersionRequest;
import com.cliphub.service.ProjectService;
import com.cliphub.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Value("${app.storage.root}")
    private String storageRoot;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("项目创建成功", projectService.createProject(CurrentUserUtil.current(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(projectService.listProjects(CurrentUserUtil.current())));
    }

    @PostMapping("/{projectId}/materials/{materialId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bindMaterial(@PathVariable Long projectId,
                                                                         @PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.bindMaterial(CurrentUserUtil.current(), projectId, materialId)));
    }

    @PostMapping("/{projectId}/import-collection/{collectionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importFromCollection(@PathVariable Long projectId,
                                                                                  @PathVariable Long collectionId) {
        return ResponseEntity.ok(ApiResponse.ok("素材集导入成功",
                projectService.importFromCollection(CurrentUserUtil.current(), projectId, collectionId)));
    }

    @PostMapping("/{projectId}/versions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveVersion(@PathVariable Long projectId,
                                                                        @Valid @RequestBody SaveVersionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("项目版本保存成功",
                projectService.saveVersion(CurrentUserUtil.current(), projectId, request)));
    }

    @GetMapping("/{projectId}/versions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> versions(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.listVersions(CurrentUserUtil.current(), projectId)));
    }

    @PostMapping("/{projectId}/rollback/{versionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rollback(@PathVariable Long projectId,
                                                                     @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.ok("版本回滚成功",
                projectService.rollback(CurrentUserUtil.current(), projectId, versionId)));
    }

    @PostMapping("/{projectId}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> export(@PathVariable Long projectId,
                                                                   @RequestParam(required = false) String format) {
        return ResponseEntity.ok(ApiResponse.ok("项目导出成功",
                projectService.exportProject(CurrentUserUtil.current(), projectId, format)));
    }

    @GetMapping("/{projectId}/exports")
    public ResponseEntity<InputStreamResource> downloadExport(@PathVariable Long projectId,
                                                              @RequestParam String path) throws FileNotFoundException {
        File file = Path.of(storageRoot, "exports", String.valueOf(projectId), path).toFile();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.getName()).build());
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(new FileInputStream(file)));
    }

    @PostMapping("/{projectId}/collaborators")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addCollaborator(@PathVariable Long projectId,
                                                                             @Valid @RequestBody CollaboratorRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("协作成员添加成功",
                projectService.addCollaborator(CurrentUserUtil.current(), projectId, request)));
    }

    @DeleteMapping("/{projectId}/collaborators/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(@PathVariable Long projectId,
                                                                @PathVariable Long userId) {
        projectService.removeCollaborator(CurrentUserUtil.current(), projectId, userId);
        return ResponseEntity.ok(ApiResponse.ok("协作成员移除成功", null));
    }
}
