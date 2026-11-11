package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng assessments (Spec v2 — bảng 14; US-T06, US-T13).
 * Phạm vi: khóa học / chủ đề / bài học (không đồng thời).
 * Tổng điểm tối đa = tổng weight của các câu hỏi (AC-T06.4) — tính động, không lưu cột.
 */
@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "open_at", nullable = false)
    private OffsetDateTime openAt;

    @Column(name = "close_at", nullable = false)
    private OffsetDateTime closeAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AssessmentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Assessment() {
    }

    public Assessment(UUID courseId, String title, int durationMinutes, int maxAttempts,
                      OffsetDateTime openAt, OffsetDateTime closeAt) {
        this.courseId = courseId;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.maxAttempts = maxAttempts;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.status = AssessmentStatus.DRAFT;
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

    public void update(String title, Integer durationMinutes, Integer maxAttempts,
                       OffsetDateTime openAt, OffsetDateTime closeAt,
                       UUID topicId, UUID lessonId, UUID actorId) {
        if (title != null) {
            this.title = title;
        }
        if (durationMinutes != null) {
            this.durationMinutes = durationMinutes;
        }
        if (maxAttempts != null) {
            this.maxAttempts = maxAttempts;
        }
        if (openAt != null) {
            this.openAt = openAt;
        }
        if (closeAt != null) {
            this.closeAt = closeAt;
        }
        this.topicId = topicId;
        this.lessonId = lessonId;
        this.updatedBy = actorId;
    }

    public void publish(UUID actorId) {
        this.status = AssessmentStatus.PUBLISHED;
        this.updatedBy = actorId;
    }

    public void unpublish(UUID actorId) {
        this.status = AssessmentStatus.DRAFT;
        this.updatedBy = actorId;
    }

    public void markDeleted(UUID actorId) {
        this.deletedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public OffsetDateTime getOpenAt() {
        return openAt;
    }

    public OffsetDateTime getCloseAt() {
        return closeAt;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
