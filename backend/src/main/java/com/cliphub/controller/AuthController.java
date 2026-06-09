package com.cliphub.controller;

import com.cliphub.common.ApiResponse;
import com.cliphub.dto.*;
import com.cliphub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("注册成功", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("登录成功", authService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            authService.logout(auth.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.ok("退出成功", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String resetToken = authService.createResetToken(request);
        return ResponseEntity.ok(ApiResponse.ok("已生成重置凭证", Map.of("resetToken", resetToken)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("密码重置成功", null));
    }
}
