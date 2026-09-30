package com.educmind.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request body cho POST /api/v1/invite-codes — Module01_Auth_SchoolClass_FINAL.md, mục A.7.
 */
public class CreateInviteCodeRequest {

    @NotNull
    private UUID classId;

    @Positive
    private int maxUses = 1;

    private OffsetDateTime expiresAt;

    public UUID getClassId() {
        return classId;
    }

    public void setClassId(UUID classId) {
        this.classId = classId;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
