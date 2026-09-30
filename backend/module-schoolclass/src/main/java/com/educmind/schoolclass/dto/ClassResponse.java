package com.educmind.schoolclass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response dùng chung cho create/get/update/list/delete — Module01_Auth_SchoolClass_FINAL.md, mục B.6. */
public class ClassResponse {

    private final UUID id;
    private final UUID schoolId;
    private final String name;
    private final String gradeLevel;
    private final String academicYear;
    private final UUID ownerTeacherId;
    private final String status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    /** [MỚI] null = lớp đang hoạt động bình thường; có giá trị = lớp đã bị soft-delete. */
    private final OffsetDateTime deletedAt;

    public ClassResponse(
            UUID id, UUID schoolId, String name, String gradeLevel, String academicYear,
            UUID ownerTeacherId, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            OffsetDateTime deletedAt
    ) {
        this.id = id;
        this.schoolId = schoolId;
        this.name = name;
        this.gradeLevel = gradeLevel;
        this.academicYear = academicYear;
        this.ownerTeacherId = ownerTeacherId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public String getName() {
        return name;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public UUID getOwnerTeacherId() {
        return ownerTeacherId;
    }

    public String getStatus() {
        return status;
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
}
