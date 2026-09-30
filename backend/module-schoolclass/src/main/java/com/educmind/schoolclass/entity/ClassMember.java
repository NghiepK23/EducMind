package com.educmind.schoolclass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity `class_members` — Module01_Auth_SchoolClass_FINAL.md, mục B.4.
 *
 * Đây chính là bảng nắm giữ khái niệm "đồng giảng dạy" (co-teacher) đã dùng xuyên suốt
 * toàn dự án (PATCH_4Decisions.md, PATCH 2): 1 user có mặt trong bảng này với
 * member_role=TEACHER cho 1 class, nhưng KHÔNG phải class.ownerTeacherId, chính là
 * "giáo viên đồng giảng dạy" của các Course link với class đó (định nghĩa quyền hạn nằm ở
 * module-course, không phải ở đây — bảng này chỉ lưu QUAN HỆ THÀNH VIÊN thô).
 *
 * {@link #userId} là cross-module reference sang module-auth (bảng users) — UUID thô.
 */
@Entity
@Table(name = "class_members", uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "user_id"}))
public class ClassMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Same-module reference (classes.id). */
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    /** Cross-module reference (module-auth.users.id). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private MemberRole memberRole;

    /**
     * updatable = false [CỐ Ý, giữ nguyên từ thiết kế gốc]: joined_at chỉ ghi 1 lần lúc
     * INSERT đầu tiên, không đổi khi UPDATE (kể cả khi rejoin sau khi đã left_at) — Hibernate
     * tự động bỏ qua cột này khi sinh câu UPDATE. Vì vậy khi xử lý "rejoin" ở ClassService,
     * KHÔNG cố set lại joinedAt, chỉ set lại leftAt = null.
     */
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    protected ClassMember() {
    }

    public ClassMember(UUID classId, UUID userId, MemberRole memberRole) {
        this.classId = classId;
        this.userId = userId;
        this.memberRole = memberRole;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = OffsetDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getClassId() {
        return classId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MemberRole getMemberRole() {
        return memberRole;
    }

    /** [MỚI] Cần cho case rejoin — user có thể quay lại lớp với role khác lần trước. */
    public void setMemberRole(MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public OffsetDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(OffsetDateTime leftAt) {
        this.leftAt = leftAt;
    }
}
