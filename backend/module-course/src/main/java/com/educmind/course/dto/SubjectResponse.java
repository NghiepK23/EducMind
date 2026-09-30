package com.educmind.course.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response dùng chung cho create/get/update/list/delete — SubjectController. */
public class SubjectResponse {

    private final UUID id;
    private final UUID schoolId;
    private final String name;
    private final String code;
    private final String description;
    private final String status;
    private final UUID createdBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    /** null = subject đang hoạt động bình thường; có giá trị = subject đã bị soft-delete. */
    private final OffsetDateTime deletedAt;

    public SubjectResponse(
            UUID id, UUID schoolId, String name, String code, String description,
            String status, UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            OffsetDateTime deletedAt
    ) {
        this.id = id;
        this.schoolId = schoolId;
        this.name = name;
        this.code = code;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
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

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
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
}
