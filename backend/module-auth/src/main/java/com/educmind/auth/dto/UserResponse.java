package com.educmind.auth.dto;

import com.educmind.auth.entity.User;

import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }
}
