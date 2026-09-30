package com.educmind.course.dto;

import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.PublishStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class LessonResponse {

    private final UUID id;
    private final UUID courseId;
    private final String title;
    private final String description;
    private final List<LearningObjective> learningObjectives;
    private final int orderIndex;
    private final PublishStatus publishStatus;
    private final OffsetDateTime publishedAt;
    private final UUID createdBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime deletedAt;

    public LessonResponse(UUID id, UUID courseId, String title, String description,
                           List<LearningObjective> learningObjectives, int orderIndex,
                           PublishStatus publishStatus, OffsetDateTime publishedAt,
                           UUID createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                           OffsetDateTime deletedAt) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.learningObjectives = learningObjectives;
        this.orderIndex = orderIndex;
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

    public List<LearningObjective> getLearningObjectives() {
        return learningObjectives;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public PublishStatus getPublishStatus() {
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
