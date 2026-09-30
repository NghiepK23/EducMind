package com.educmind.auth.controller;

import com.educmind.auth.dto.ChangePasswordForcedRequest;
import com.educmind.auth.dto.LoginRequest;
import com.educmind.auth.dto.LoginResponse;
import com.educmind.auth.dto.PasswordResetConfirmRequest;
import com.educmind.auth.dto.PasswordResetRequestRequest;
import com.educmind.auth.dto.RefreshRequest;
import com.educmind.auth.dto.RefreshResponse;
import com.educmind.auth.dto.RegisterRequest;
import com.educmind.auth.dto.RegisterResponse;
import com.educmind.auth.service.AuthService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Module01_Auth_SchoolClass_FINAL.md, mục A.6.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/auth/refresh — khong can JWT (da permitAll o SecurityConfig),
     * xac thuc bang chinh refresh token gui trong body.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/auth/logout — can JWT hop le (Yes theo spec A.6), nhung ban than logic
     * revoke chi can refresh token trong body, khong can doc userId tu JWT.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * POST /api/v1/auth/password-reset/request — khong can JWT (permitAll o SecurityConfig).
     * Luon tra ve 200 du email co ton tai hay khong (chong user enumeration).
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestRequest request
    ) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * POST /api/v1/auth/password-reset/confirm — khong can JWT, xac thuc bang chinh
     * reset token gui trong body.
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * POST /api/v1/auth/change-password-forced — can JWT hop le. Chi user co
     * must_change_password=true moi toi duoc endpoint khac ngoai endpoint nay
     * (da chan boi JwtAuthenticationFilter). Lay userId tu SecurityContext,
     * KHONG nhan tu body, giong pattern UserController.getCurrentUser().
     */
    @PostMapping("/change-password-forced")
    public ResponseEntity<ApiResponse<Void>> changePasswordForced(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordForcedRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        authService.changePasswordForced(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
