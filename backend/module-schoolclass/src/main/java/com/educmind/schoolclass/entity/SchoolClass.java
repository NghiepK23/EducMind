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

/** Bảng classes (Spec v2, mục C — bảng 2). Một lớp có đúng một giáo viên phụ trách. */
@Entity
@Table(name = "classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    private Short grade;

    @Column(name = "school_year", length = 20)
    private String schoolYear;

    @Column(name = "owner_teacher_id", nullable = false)
    private UUID ownerTeacherId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected SchoolClass() {
    }

    public SchoolClass(String name, Short grade, String schoolYear, UUID ownerTeacherId) {
        this.name = name;
        this.grade = grade;
        this.schoolYear = schoolYear;
        this.ownerTeacherId = ownerTeacherId;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setGrade(Short grade) {
        this.grade = grade;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void markDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Short getGrade() {
        return grade;
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public UUID getOwnerTeacherId() {
        return ownerTeacherId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
