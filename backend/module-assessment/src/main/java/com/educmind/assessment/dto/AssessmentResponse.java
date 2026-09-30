package com.educmind.assessment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AssessmentResponse {

    private final UUID id;
    private final UUID courseId;
    private final String title;
    private final String description;
    private final Integer durationMinutes;
    private final int maxAttempts;
    private final Object blueprintConfig;
    private final OffsetDateTime opensAt;
    private final OffsetDateTime closesAt;
    private final String publishStatus;
    private final OffsetDateTime publishedAt;
    private final UUID createdBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime deletedAt;

    public AssessmentResponse(UUID id, UUID courseId, String title, String description, Integer durationMinutes,
                               int maxAttempts, Object blueprintConfig, OffsetDateTime opensAt,
                               OffsetDateTime closesAt, String publishStatus, OffsetDateTime publishedAt,
                               UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                               OffsetDateTime deletedAt) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.maxAttempts = maxAttempts;
        this.blueprintConfig = blueprintConfig;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.publishStatus = publishStatus;
        this.publishedAt = publishedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Object getBlueprintConfig() {
        return blueprintConfig;
    }

    public OffsetDateTime getOpensAt() {
        return opensAt;
    }

    public OffsetDateTime getClosesAt() {
        return closesAt;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
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
