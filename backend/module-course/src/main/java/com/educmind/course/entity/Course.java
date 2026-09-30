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
 * Entity ứng với bảng `courses` — Module03_Course_Lesson_Resource_FINAL.md, mục C.4.
 *
 * {@link #schoolId} là same-module reference thật ra là cross-module tới module-schoolclass
 * (bảng schools) — UUID thô, không dùng @ManyToOne, đúng nguyên tắc "raw UUID cho FK xuyên
 * module" đã chốt xuyên suốt dự án (tránh circular Maven dependency).
 * {@link #createdBy} là cross-module reference sang module-auth (bảng users).
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Cross-module reference (module-schoolclass.schools.id). */
    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Cross-module reference (module-course.subjects.id) — bắt buộc từ khi đổi kiến trúc, xem V3 migration. */
    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    /** Cross-module reference (module-auth.users.id) — phải là TEACHER. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Course() {
    }

    public Course(UUID schoolId, String name, UUID subjectId, UUID createdBy) {
        this.schoolId = schoolId;
        this.name = name;
        this.subjectId = subjectId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public UUID getCreatedBy() {
        return createdBy;
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
