package com.educmind.auth.controller;

import com.educmind.auth.dto.ChangePasswordRequest;
import com.educmind.auth.dto.LoginRequest;
import com.educmind.auth.dto.LoginResponse;
import com.educmind.auth.dto.PasswordResetConfirmRequest;
import com.educmind.auth.dto.PasswordResetRequestRequest;
import com.educmind.auth.service.AuthService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Xác thực — Spec v2: US-G01 (đăng nhập/đăng xuất), US-G02 (quên mật khẩu), US-G04 (đổi mật khẩu).
 * Không còn đăng ký tự do / invite code (Spec v2 chỉ có 2 vai trò và giáo viên thêm học sinh).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** AC-G02.1: luôn trả cùng một thông báo dù email có tồn tại hay không. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestReset(
            @Valid @RequestBody PasswordResetRequestRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Nếu email tồn tại, hệ thống đã gửi liên kết đặt lại mật khẩu")));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Đặt lại mật khẩu thành công")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Đổi mật khẩu thành công")));
    }
}
