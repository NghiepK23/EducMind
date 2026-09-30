package com.educmind.auth.dto;

import com.educmind.auth.entity.Role;

import java.util.UUID;

/**
 * Response cho POST /api/v1/users — theo spec Module 1, mục A.7.
 */
public class CreateUserResponse {

    private final UUID userId;
    private final Role role;
    private final String temporaryPassword;

    public CreateUserResponse(UUID userId, Role role, String temporaryPassword) {
        this.userId = userId;
        this.role = role;
        this.temporaryPassword = temporaryPassword;
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }
}
