package com.educmind.auth.dto;

import com.educmind.auth.entity.Role;

import java.util.UUID;

/**
 * Response cho POST /api/v1/auth/register — theo spec Module 1, mục A.7.
 */
public class RegisterResponse {

    private final UUID userId;
    private final Role role;
    private final UUID schoolId;
    private final UUID classId;

    public RegisterResponse(UUID userId, Role role, UUID schoolId, UUID classId) {
        this.userId = userId;
        this.role = role;
        this.schoolId = schoolId;
        this.classId = classId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public UUID getClassId() {
        return classId;
    }
}
