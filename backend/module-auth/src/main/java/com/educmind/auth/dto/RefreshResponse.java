package com.educmind.auth.dto;

/**
 * Response cho POST /api/v1/auth/refresh.
 * [DECIDED — xac nhan voi Nghiep] Ap dung refresh token rotation: moi lan goi /refresh,
 * token cu bi revoke ngay va client nhan ve CA access token lan refresh token MOI.
 * Khong co field mustChangePassword nhu LoginResponse vi day khong phai luc dang nhap dau tien.
 */
public class RefreshResponse {

    private final String accessToken;
    private final String refreshToken;

    public RefreshResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
