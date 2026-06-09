package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.UpdateProfileRequest;
import com.cliphub.dto.UserRoleUpdateRequest;
import com.cliphub.entity.User;
import com.cliphub.service.UserService;
import com.cliphub.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> profile() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(CurrentUserUtil.current())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("个人资料更新成功",
                userService.updateProfile(CurrentUserUtil.current(), request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers()));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateRole(@PathVariable Long userId,
                                                        @Valid @RequestBody UserRoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("角色更新成功",
                userService.updateRole(userId, request.getRole(), CurrentUserUtil.current())));
    }
}
