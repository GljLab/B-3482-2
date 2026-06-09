package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.MaterialSearchRequest;
import com.cliphub.dto.MaterialUpdateRequest;
import com.cliphub.dto.MaterialUploadMetaRequest;
import com.cliphub.dto.ShareRequest;
import com.cliphub.entity.Material;
import com.cliphub.service.MaterialService;
import com.cliphub.util.CurrentUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Material>> upload(@ModelAttribute MaterialUploadMetaRequest request,
                                                        @RequestPart("file") MultipartFile file) {
        Material material = materialService.upload(CurrentUserUtil.current(), request, file);
        return ResponseEntity.ok(ApiResponse.ok("素材上传成功", material));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> search(MaterialSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(materialService.search(CurrentUserUtil.current(), request)));
    }

    @GetMapping("/{materialId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok(materialService.detail(CurrentUserUtil.current(), materialId)));
    }

    @PutMapping("/{materialId}")
    public ResponseEntity<ApiResponse<Material>> update(@PathVariable Long materialId,
                                                        @RequestBody MaterialUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("素材更新成功",
                materialService.update(CurrentUserUtil.current(), materialId, request)));
    }

    @DeleteMapping("/{materialId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long materialId) {
        materialService.delete(CurrentUserUtil.current(), materialId);
        return ResponseEntity.ok(ApiResponse.ok("素材删除成功", null));
    }

    @PostMapping("/{materialId}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> favorite(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.ok("收藏状态更新成功",
                materialService.toggleFavorite(CurrentUserUtil.current(), materialId)));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> favorites() {
        return ResponseEntity.ok(ApiResponse.ok(materialService.listFavorites(CurrentUserUtil.current())));
    }

    @PostMapping("/{materialId}/share")
    public ResponseEntity<ApiResponse<Map<String, Object>>> share(@PathVariable Long materialId,
                                                                  @RequestBody(required = false) ShareRequest request) {
        ShareRequest payload = request == null ? new ShareRequest() : request;
        return ResponseEntity.ok(ApiResponse.ok("分享链接创建成功",
                materialService.createShare(CurrentUserUtil.current(), materialId, payload)));
    }

    @GetMapping("/share/{code}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> byShareCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(materialService.getByShareCode(code)));
    }

    @GetMapping("/{materialId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long materialId,
                                                        @RequestParam(required = false) String quality,
                                                        @RequestParam(required = false) String format) throws FileNotFoundException {
        String path = materialService.resolveDownloadPath(CurrentUserUtil.current(), materialId, quality, format);
        File file = new File(path);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.getName()).build());
        if (quality != null) {
            headers.add("X-Requested-Quality", quality);
        }
        if (format != null) {
            headers.add("X-Requested-Format", format);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .body(new InputStreamResource(new FileInputStream(file)));
    }

    @GetMapping("/{materialId}/preview")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long materialId) throws FileNotFoundException {
        String path = materialService.resolvePreviewPath(CurrentUserUtil.current(), materialId);
        File file = new File(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .body(new InputStreamResource(new FileInputStream(file)));
    }
}
