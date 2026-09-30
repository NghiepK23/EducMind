package com.educmind.course.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response dùng chung cho create/get/update/list/delete — Module03_Course_Lesson_Resource_FINAL.md, mục C.6. */
public class CourseResponse {

    private final UUID id;
    private final UUID schoolId;
    private final String name;
    private final String description;
    private final java.util.UUID subjectId;
    private final UUID createdBy;
    private final String status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    /** null = course đang hoạt động bình thường; có giá trị = course đã bị soft-delete. */
    private final OffsetDateTime deletedAt;

    public CourseResponse(
            UUID id, UUID schoolId, String name, String description, UUID subjectId,
            UUID createdBy, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            OffsetDateTime deletedAt
    ) {
        this.id = id;
        this.schoolId = schoolId;
        this.name = name;
        this.description = description;
        this.subjectId = subjectId;
        this.createdBy = createdBy;
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

    public String getDescription() {
        return description;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public UUID getCreatedBy() {
        return createdBy;
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
