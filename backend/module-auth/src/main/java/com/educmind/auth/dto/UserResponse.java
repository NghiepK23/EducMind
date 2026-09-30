package com.educmind.auth.dto;

import com.educmind.auth.entity.Role;
import com.educmind.auth.entity.UserStatus;

import java.util.UUID;

/**
 * Response cho GET /api/v1/users/me — Module01_Auth_SchoolClass_FINAL.md, mục A.6.
 * Không trả passwordHash — không bao giờ lộ thông tin nhạy cảm ra API.
 */
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private Role role;
    private UUID schoolId;
    private UserStatus status;
    private boolean mustChangePassword;

    public UserResponse(
            UUID id,
            String email,
            String fullName,
            Role role,
            UUID schoolId,
            UserStatus status,
            boolean mustChangePassword
    ) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.schoolId = schoolId;
        this.status = status;
        this.mustChangePassword = mustChangePassword;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
