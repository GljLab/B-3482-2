package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.*;
import com.cliphub.service.CollectionService;
import com.cliphub.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@Valid @RequestBody CollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("素材集创建成功",
                collectionService.create(CurrentUserUtil.current(), request)));
    }

    @PutMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable Long collectionId,
                                                                    @RequestBody CollectionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("素材集更新成功",
                collectionService.update(CurrentUserUtil.current(), collectionId, request)));
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long collectionId,
                                                     @RequestParam(defaultValue = "false") boolean removeFavorites) {
        collectionService.delete(CurrentUserUtil.current(), collectionId, removeFavorites);
        return ResponseEntity.ok(ApiResponse.ok("素材集删除成功", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String visibility) {
        return ResponseEntity.ok(ApiResponse.ok(
                collectionService.list(CurrentUserUtil.current(), keyword, sortBy, visibility)));
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable Long collectionId) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.detail(CurrentUserUtil.current(), collectionId)));
    }

    @PostMapping("/{collectionId}/materials/{materialId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMaterial(@PathVariable Long collectionId,
                                                                         @PathVariable Long materialId,
                                                                         @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok("素材已加入素材集",
                collectionService.addMaterial(CurrentUserUtil.current(), collectionId, materialId, note)));
    }

    @DeleteMapping("/{collectionId}/materials/{materialId}")
    public ResponseEntity<ApiResponse<Void>> removeMaterial(@PathVariable Long collectionId,
                                                             @PathVariable Long materialId) {
        collectionService.removeMaterial(CurrentUserUtil.current(), collectionId, materialId);
        return ResponseEntity.ok(ApiResponse.ok("素材已从素材集移除", null));
    }

    @PutMapping("/{collectionId}/materials/{materialId}/note")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNote(@PathVariable Long collectionId,
                                                                        @PathVariable Long materialId,
                                                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("备注更新成功",
                collectionService.updateMaterialNote(CurrentUserUtil.current(), collectionId, materialId,
                        body.get("note"))));
    }

    @PutMapping("/{collectionId}/materials/sort")
    public ResponseEntity<ApiResponse<Void>> sortMaterials(@PathVariable Long collectionId,
                                                            @RequestBody MaterialSortRequest request) {
        collectionService.sortMaterials(CurrentUserUtil.current(), collectionId, request);
        return ResponseEntity.ok(ApiResponse.ok("排序已保存", null));
    }

    @PostMapping("/materials/{materialId}/set-collections")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setMaterialCollections(
            @PathVariable Long materialId,
            @RequestBody CollectionMaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("素材集归属已更新",
                collectionService.setMaterialCollections(CurrentUserUtil.current(), materialId, request.getCollectionIds())));
    }

    @GetMapping("/materials/{materialId}/collections")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMaterialCollections(
            @PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(
                collectionService.getMaterialCollections(CurrentUserUtil.current(), materialId)));
    }

    @PostMapping("/{collectionId}/share")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createShare(@PathVariable Long collectionId,
                                                                         @RequestBody(required = false) CollectionShareRequest request) {
        CollectionShareRequest payload = request == null ? new CollectionShareRequest() : request;
        return ResponseEntity.ok(ApiResponse.ok("分享链接创建成功",
                collectionService.createShare(CurrentUserUtil.current(), collectionId, payload)));
    }

    @GetMapping("/share/{code}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByShareCode(
            @PathVariable String code,
            @RequestParam(required = false) String password) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.getByShareCode(code, password)));
    }

    @DeleteMapping("/shares/{shareId}")
    public ResponseEntity<ApiResponse<Void>> revokeShare(@PathVariable Long shareId) {
        collectionService.revokeShare(CurrentUserUtil.current(), shareId);
        return ResponseEntity.ok(ApiResponse.ok("分享链接已失效", null));
    }

    @GetMapping("/{collectionId}/shares")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listShares(@PathVariable Long collectionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                collectionService.listShares(CurrentUserUtil.current(), collectionId)));
    }

    @PutMapping("/sort")
    public ResponseEntity<ApiResponse<Void>> sortCollections(@RequestBody CollectionSortRequest request) {
        collectionService.sortCollections(CurrentUserUtil.current(), request);
        return ResponseEntity.ok(ApiResponse.ok("素材集排序已保存", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.collectionStats(CurrentUserUtil.current())));
    }
}
