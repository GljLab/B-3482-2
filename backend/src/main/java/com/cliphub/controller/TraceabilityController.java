package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.CleanupUnusedRequest;
import com.cliphub.dto.CopyProjectRequest;
import com.cliphub.dto.DeleteImpactRequest;
import com.cliphub.dto.MaterialSourceRequest;
import com.cliphub.dto.MaterialTransferRequest;
import com.cliphub.service.TraceabilityService;
import com.cliphub.util.CurrentUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traceability")
@RequiredArgsConstructor
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    @GetMapping("/materials/{materialId}/timeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> materialTimeline(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getMaterialUsageTimeline(
                CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/materials/{materialId}/ref-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> materialRefStats(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getMaterialReferenceStats(
                CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/materials/{materialId}/projects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> materialProjects(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getMaterialProjects(
                CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/materials/{materialId}/collections")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> materialCollections(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getMaterialCollections(
                CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/materials/{materialId}/mod-history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> materialModHistory(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getMaterialModificationHistory(
                CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/materials/{materialId}/transfer-history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> materialTransferHistory(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getTransferHistory(
                CurrentUserUtil.current(), materialId)));
    }

    @PutMapping("/materials/{materialId}/source-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setMaterialSource(@PathVariable Long materialId,
                                                                              @RequestBody MaterialSourceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("来源信息已更新",
                traceabilityService.setMaterialSourceInfo(CurrentUserUtil.current(), materialId, request)));
    }

    @GetMapping("/materials/{materialId}/delete-impact")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteImpact(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.assessDeleteImpact(
                CurrentUserUtil.current(), materialId)));
    }

    @DeleteMapping("/materials/{materialId}/with-strategy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteWithStrategy(@PathVariable Long materialId,
                                                                               @RequestBody(required = false) DeleteImpactRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("删除操作已完成",
                traceabilityService.deleteMaterialWithStrategy(CurrentUserUtil.current(), materialId, request)));
    }

    @GetMapping("/recycle-bin")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listRecycleBin() {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.listRecycleBin(CurrentUserUtil.current())));
    }

    @PostMapping("/recycle-bin/{materialId}/restore")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restoreFromRecycle(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok("素材已恢复",
                traceabilityService.restoreFromRecycleBin(CurrentUserUtil.current(), materialId)));
    }

    @DeleteMapping("/recycle-bin/{materialId}/permanent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> permanentDelete(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok("素材已永久删除",
                traceabilityService.permanentlyDeleteFromRecycleBin(CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/projects/{projectId}/material-inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> projectInventory(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getProjectMaterialInventory(
                CurrentUserUtil.current(), projectId)));
    }

    @GetMapping("/projects/{projectId}/contributors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> projectContributors(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getProjectMaterialContributors(
                CurrentUserUtil.current(), projectId)));
    }

    @GetMapping("/projects/{projectId}/availability-check")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> projectAvailability(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.checkProjectMaterialAvailability(
                CurrentUserUtil.current(), projectId)));
    }

    @GetMapping("/projects/{projectId}/inventory-export")
    public ResponseEntity<ByteArrayResource> exportInventory(@PathVariable Long projectId,
                                                              @RequestParam(required = false) String format) {
        Map<String, Object> export = traceabilityService.exportProjectMaterialInventory(
                CurrentUserUtil.current(), projectId, format);
        String content = (String) export.get("content");
        String filename = (String) export.get("filename");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(resource);
    }

    @GetMapping("/projects/{projectId}/versions/{versionId}/material-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> versionMaterialCheck(@PathVariable Long projectId,
                                                                                 @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.checkVersionMaterialsAvailability(
                CurrentUserUtil.current(), projectId, versionId)));
    }

    @GetMapping("/projects/{projectId}/recommend-materials")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> recommendMaterials(@PathVariable Long projectId,
                                                                                     @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.recommendMaterialsForProject(
                CurrentUserUtil.current(), projectId, limit)));
    }

    @PostMapping("/projects/{sourceProjectId}/copy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> copyProject(@PathVariable Long sourceProjectId,
                                                                        @RequestBody CopyProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("项目复制成功",
                traceabilityService.copyProject(CurrentUserUtil.current(), sourceProjectId, request)));
    }

    @GetMapping("/rankings/hot-materials")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> hotMaterials(
            @RequestParam(required = false) String dimension,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getHotMaterialsRanking(
                CurrentUserUtil.current(), dimension, limit)));
    }

    @GetMapping("/rankings/high-reuse")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> highReuseMaterials(
            @RequestParam(defaultValue = "2") int threshold) {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getHighReuseMaterials(
                CurrentUserUtil.current(), threshold)));
    }

    @GetMapping("/materials/unused")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> unusedMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getUnusedMaterials(
                CurrentUserUtil.current())));
    }

    @GetMapping("/team/contribution-stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> teamContribution() {
        return ResponseEntity.ok(ApiResponse.ok(traceabilityService.getTeamContributionStats(
                CurrentUserUtil.current())));
    }

    @PostMapping("/materials/transfer-ownership")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferOwnership(@RequestBody MaterialTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("所有权转移完成",
                traceabilityService.transferMaterialsOwnership(CurrentUserUtil.current(), request)));
    }

    @PostMapping("/admin/cleanup-unused")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminCleanup(@RequestBody CleanupUnusedRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("清理任务已完成",
                traceabilityService.adminCleanupUnusedMaterials(CurrentUserUtil.current(), request)));
    }
}
