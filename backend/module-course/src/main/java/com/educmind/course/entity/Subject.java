package com.educmind.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `subjects` — môn học do SCHOOL_ADMIN tạo sẵn cho trường
 * mình (vd: "Tin học", "Toán"). Giáo viên chọn 1 subject khi tạo Course.
 *
 * Đây là entity MỚI, phát sinh từ thay đổi kiến trúc: Course không còn N-N với
 * Class nữa, mà thuộc 1 subject cụ thể (xem checkpoint "THAY ĐỔI KIẾN TRÚC LỚN").
 *
 * {@link #schoolId} là cross-module reference sang module-schoolclass (bảng
 * schools) — raw UUID, không dùng @ManyToOne, đúng nguyên tắc "raw UUID cho FK
 * xuyên module" đã chốt xuyên suốt dự án.
 * {@link #createdBy} là cross-module reference sang module-auth (bảng users) —
 * phải là SCHOOL_ADMIN (Super Admin không tham gia nghiệp vụ trường).
 */
@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Cross-module reference (module-schoolclass.schools.id). */
    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /** Cross-module reference (module-auth.users.id) — phải là SCHOOL_ADMIN. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Subject() {
    }

    public Subject(UUID schoolId, String name, UUID createdBy) {
        this.schoolId = schoolId;
        this.name = name;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
