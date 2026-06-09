package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.CategoryRequest;
import com.cliphub.dto.NotificationRequest;
import com.cliphub.dto.SettingRequest;
import com.cliphub.dto.TagRequest;
import com.cliphub.service.AdminService;
import com.cliphub.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/api/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCategories()));
    }

    @GetMapping("/api/tags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tags() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listTags()));
    }

    @GetMapping("/api/notifications/public")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> publicNotifications() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPublicNotifications()));
    }

    @GetMapping("/api/admin/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> adminCategories() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCategories()));
    }

    @PostMapping("/api/admin/categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("分类创建成功",
                adminService.saveCategory(request, null, CurrentUserUtil.current())));
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCategory(@PathVariable Long id,
                                                                           @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("分类更新成功",
                adminService.saveCategory(request, id, CurrentUserUtil.current())));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id, CurrentUserUtil.current());
        return ResponseEntity.ok(ApiResponse.ok("分类删除成功", null));
    }

    @GetMapping("/api/admin/tags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> adminTags() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listTags()));
    }

    @PostMapping("/api/admin/tags")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTag(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("标签创建成功",
                adminService.saveTag(request, null, CurrentUserUtil.current())));
    }

    @PutMapping("/api/admin/tags/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTag(@PathVariable Long id,
                                                                      @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("标签更新成功",
                adminService.saveTag(request, id, CurrentUserUtil.current())));
    }

    @DeleteMapping("/api/admin/tags/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        adminService.deleteTag(id, CurrentUserUtil.current());
        return ResponseEntity.ok(ApiResponse.ok("标签删除成功", null));
    }

    @GetMapping("/api/admin/settings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> settings() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listSettings()));
    }

    @PostMapping("/api/admin/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveSetting(@Valid @RequestBody SettingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("配置保存成功",
                adminService.saveSetting(request, CurrentUserUtil.current())));
    }

    @GetMapping("/api/admin/notifications")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> notifications() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listNotifications()));
    }

    @PostMapping("/api/admin/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNotification(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("通知创建成功",
                adminService.saveNotification(request, null, CurrentUserUtil.current())));
    }

    @PutMapping("/api/admin/notifications/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotification(@PathVariable Long id,
                                                                                @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("通知更新成功",
                adminService.saveNotification(request, id, CurrentUserUtil.current())));
    }

    @DeleteMapping("/api/admin/notifications/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        adminService.deleteNotification(id, CurrentUserUtil.current());
        return ResponseEntity.ok(ApiResponse.ok("通知删除成功", null));
    }
}
