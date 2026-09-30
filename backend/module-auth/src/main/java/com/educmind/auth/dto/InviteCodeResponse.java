package com.educmind.auth.dto;

import com.educmind.auth.entity.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response cho invite code — dùng chung cho POST (tạo mới) và GET (danh sách).
 * Theo Module01_Auth_SchoolClass_FINAL.md, mục A.7.
 */
public class InviteCodeResponse {

    private UUID inviteCodeId;
    private String code;
    private UUID classId;
    private Role intendedRole;
    private int maxUses;
    private int usedCount;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime revokedAt;

    public InviteCodeResponse(
            UUID inviteCodeId, String code, UUID classId, Role intendedRole,
            int maxUses, int usedCount, OffsetDateTime expiresAt,
            OffsetDateTime createdAt, OffsetDateTime revokedAt
    ) {
        this.inviteCodeId = inviteCodeId;
        this.code = code;
        this.classId = classId;
        this.intendedRole = intendedRole;
        this.maxUses = maxUses;
        this.usedCount = usedCount;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public UUID getInviteCodeId() {
        return inviteCodeId;
    }

    public String getCode() {
        return code;
    }

    public UUID getClassId() {
        return classId;
    }

    public Role getIntendedRole() {
        return intendedRole;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
