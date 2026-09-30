package com.educmind.schoolclass.entity;

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
 * Entity ứng với bảng `classes` — Module01_Auth_SchoolClass_FINAL.md, mục B.4.
 *
 * [Giải thích cho người mới] Vì sao tên class Java là "SchoolClass" chứ không phải "Class"?
 * Vì "Class" là từ khóa/kiểu dữ liệu có sẵn trong Java (java.lang.Class — đại diện cho
 * metadata của 1 class bất kỳ lúc runtime). Đặt tên entity là "Class" sẽ rất dễ gây nhầm lẫn
 * khi đọc code (import java.lang.Class vs entity Class), nên đặt "SchoolClass" cho rõ nghĩa,
 * dù tên bảng SQL thật vẫn là `classes` (khai báo qua @Table(name = "classes")).
 *
 * {@link #ownerTeacherId} và {@link #createdBy} là cross-module reference sang module-auth
 * (bảng users) — UUID thô, xem giải thích chi tiết ở com.educmind.auth.entity.User.
 */
@Entity
@Table(name = "classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Same-module reference (schools.id). */
    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "grade_level", length = 20)
    private String gradeLevel;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    /** Cross-module reference (module-auth.users.id) — giáo viên chủ nhiệm/sở hữu lớp. */
    @Column(name = "owner_teacher_id", nullable = false)
    private UUID ownerTeacherId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Cross-module reference (module-auth.users.id). */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected SchoolClass() {
    }

    public SchoolClass(UUID schoolId, String name, UUID ownerTeacherId, UUID createdBy) {
        this.schoolId = schoolId;
        this.name = name;
        this.ownerTeacherId = ownerTeacherId;
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

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public UUID getOwnerTeacherId() {
        return ownerTeacherId;
    }

    public void setOwnerTeacherId(UUID ownerTeacherId) {
        this.ownerTeacherId = ownerTeacherId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
