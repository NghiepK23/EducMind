package com.educmind.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request cho POST /api/v1/auth/refresh và POST /api/v1/auth/logout.
 * Ca 2 endpoint deu chi can refresh token tho (client gui len tu luc login) de xac dinh
 * dong revoke/rotate.
 */
public class RefreshRequest {

    @NotBlank(message = "refreshToken khong duoc de trong")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
