package com.educmind.auth.dto;

/**
 * Response cho POST /api/v1/auth/login.
 * mustChangePassword: [DECIDED — Module 1, mục A.7] frontend dùng cờ này để redirect thẳng
 * tới màn hình đổi mật khẩu bắt buộc trước khi cho vào hệ thống.
 */
public class LoginResponse {

    private final String accessToken;private final String refreshToken;
    private final boolean mustChangePassword;

    public LoginResponse(String accessToken, String refreshToken, boolean mustChangePassword) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mustChangePassword = mustChangePassword;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
