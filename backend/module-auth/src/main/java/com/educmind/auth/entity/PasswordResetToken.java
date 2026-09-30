package com.educmind.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity `password_reset_tokens` — Module01_Auth_SchoolClass_FINAL.md, mục A.4 + A.5.4.
 *
 * [DECIDED — PATCH2_TechnicalGaps.md, mục C.1] TTL = 15 phút — xem hằng số {@link #TTL}.
 * Đây là con số ĐÃ CHỐT (lịch sử: bản gốc từng mâu thuẫn 1 phút vs 30 phút, chủ dự án đã
 * xác nhận lại 15 phút là quyết định cuối) — KHÔNG tự ý đổi số này khi code, nếu cần đổi
 * phải quay lại xác nhận với chủ dự án trước.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    /** [DECIDED] Thời hạn sống của reset token — 15 phút. */
    public static final Duration TTL = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Same-module reference (users.id). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PasswordResetToken() {
    }

    /** Tạo token mới — expiresAt tự tính = now + TTL (15 phút), không truyền tay từ ngoài để tránh sai sót. */
    public PasswordResetToken(UUID userId, String tokenHash) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = OffsetDateTime.now().plus(TTL);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    /** true nếu token còn dùng được: chưa dùng, chưa hết hạn. */
    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    public void markUsed() {
        this.usedAt = OffsetDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
