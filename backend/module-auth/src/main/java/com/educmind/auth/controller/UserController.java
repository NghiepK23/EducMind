package com.educmind.auth.controller;

import com.educmind.auth.dto.CreateUserRequest;
import com.educmind.auth.dto.CreateUserResponse;
import com.educmind.auth.dto.UserResponse;
import com.educmind.auth.service.UserService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Module01_Auth_SchoolClass_FINAL.md, mục A.6.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Chỉ SCHOOL_ADMIN được tạo tài khoản GV/HS — Module01_Auth_SchoolClass_FINAL.md, mục A.9.
     * Yêu cầu @EnableMethodSecurity đã bật ở SecurityConfig để annotation này có tác dụng.
     */
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        CreateUserResponse response = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/users/me — trả thông tin user hiện tại, dựa trên userId đã được
     * JwtAuthenticationFilter xác thực và đặt vào SecurityContextHolder (Authentication.getName()).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
