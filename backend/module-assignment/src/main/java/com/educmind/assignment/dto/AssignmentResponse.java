package com.educmind.assignment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AssignmentResponse {

    private final UUID id;
    private final UUID courseId;
    private final UUID lessonId;
    private final String title;
    private final String description;
    private final OffsetDateTime dueDate;
    private final boolean allowLateSubmission;
    private final BigDecimal maxScore;
    private final String publishStatus;
    private final UUID createdBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime deletedAt;

    public AssignmentResponse(UUID id, UUID courseId, UUID lessonId, String title, String description,
                               OffsetDateTime dueDate, boolean allowLateSubmission, BigDecimal maxScore, String publishStatus,
                               UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                               OffsetDateTime deletedAt) {
        this.id = id;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.allowLateSubmission = allowLateSubmission;
        this.maxScore = maxScore;
        this.publishStatus = publishStatus;
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

    public UUID getLessonId() {
        return lessonId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public boolean isAllowLateSubmission() {
        return allowLateSubmission;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public String getPublishStatus() {
        return publishStatus;
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
