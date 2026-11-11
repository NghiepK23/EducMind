package com.educmind.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {

    public static LoginResponse of(String token, long expiresInSeconds, UserResponse user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
