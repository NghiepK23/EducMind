package com.educmind.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity `invite_codes` — Module01_Auth_SchoolClass_FINAL.md, mục A.4.
 *
 * {@link #classId} và {@link #schoolId} là cross-module reference sang module-schoolclass
 * (bảng classes/schools) — UUID thô, cùng lý do đã giải thích ở {@link User}.
 * {@link #createdBy} là same-module reference sang users.id — UUID thô.
 */
@Entity
@Table(name = "invite_codes")
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** Cross-module reference (module-schoolclass.classes.id). */
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    /** Cross-module reference (module-schoolclass.schools.id) — suy ra từ classId, lưu thêm để truy vấn nhanh. */
    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "intended_role", nullable = false, length = 20)
    private Role intendedRole = Role.STUDENT;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /** Same-module reference (users.id) — bắt buộc phải là user có role=TEACHER, validate ở Service. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected InviteCode() {
    }

    public InviteCode(String code, UUID classId, UUID schoolId, Role intendedRole, int maxUses, UUID createdBy) {
        this.code = code;
        this.classId = classId;
        this.schoolId = schoolId;
        this.intendedRole = intendedRole;
        this.maxUses = maxUses;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    /** true nếu code còn dùng được: chưa bị revoke, chưa hết lượt, chưa hết hạn. */
    public boolean isUsable() {
        if (revokedAt != null) {
            return false;
        }
        if (usedCount >= maxUses) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(OffsetDateTime.now());
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public UUID getClassId() {
        return classId;
    }

    public UUID getSchoolId() {
        return schoolId;
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

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public void incrementUsedCount() {
        this.usedCount++;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
