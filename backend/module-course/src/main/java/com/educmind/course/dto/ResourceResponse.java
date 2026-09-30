package com.educmind.course.dto;

import com.educmind.course.entity.ProcessingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ResourceResponse {

    private final UUID id;
    private final UUID courseId;
    private final UUID lessonId;
    private final String filename;
    private final String mimeType;
    private final long sizeBytes;
    private final ProcessingStatus processingStatus;
    private final UUID uploadedBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime deletedAt;

    public ResourceResponse(UUID id, UUID courseId, UUID lessonId, String filename,
                             String mimeType, long sizeBytes, ProcessingStatus processingStatus,
                             UUID uploadedBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                             OffsetDateTime deletedAt) {
        this.id = id;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.filename = filename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.processingStatus = processingStatus;
        this.uploadedBy = uploadedBy;
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

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
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
