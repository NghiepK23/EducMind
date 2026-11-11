package com.educmind.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Bảng resources (Spec v2, mục D — bảng 8). Tài liệu của khóa học hoặc của một bài học. */
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Resource() {
    }

    public Resource(UUID courseId, UUID lessonId, String fileName, String fileUrl,
                    Long fileSizeBytes, String fileType, UUID uploadedBy) {
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.fileType = fileType;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
    }

    public void markDeleted() {
        this.deletedAt = OffsetDateTime.now();
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

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFileType() {
        return fileType;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
