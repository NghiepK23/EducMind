package com.educmind.assignment.entity;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng assignments (Spec v2 — bảng 9; US-T03).
 * Phạm vi: khóa học (topicId & lessonId NULL) / chủ đề (chỉ topicId) / bài học (chỉ lessonId).
 */
@Entity
@Table(name = "assignments")
public class Assignment {

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

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "due_date", nullable = false)
    private OffsetDateTime dueDate;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "allow_late", nullable = false)
    private boolean allowLate;

    @Column(name = "late_close_date")
    private OffsetDateTime lateCloseDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AssignmentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Assignment() {
    }

    public Assignment(UUID courseId, String title, String description, OffsetDateTime dueDate,
                      BigDecimal maxScore, boolean allowLate, OffsetDateTime lateCloseDate) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.allowLate = allowLate;
        this.lateCloseDate = allowLate ? lateCloseDate : null;
        this.status = AssignmentStatus.DRAFT;
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

    public void update(String title, String description, OffsetDateTime dueDate, BigDecimal maxScore,
                       Boolean allowLate, OffsetDateTime lateCloseDate, UUID topicId, UUID lessonId,
                       UUID actorId) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (dueDate != null) {
            this.dueDate = dueDate;
        }
        if (maxScore != null) {
            this.maxScore = maxScore;
        }
        boolean late = allowLate == null ? this.allowLate : allowLate;
        if (!late) {
            this.allowLate = false;
            this.lateCloseDate = null;
        } else {
            this.allowLate = true;
            OffsetDateTime close = lateCloseDate == null ? this.lateCloseDate : lateCloseDate;
            this.lateCloseDate = close;
        }
        this.topicId = topicId;
        this.lessonId = lessonId;
        this.updatedBy = actorId;
    }

    public void publish(UUID actorId) {
        this.status = AssignmentStatus.PUBLISHED;
        this.updatedBy = actorId;
    }

    public void unpublish(UUID actorId) {
        this.status = AssignmentStatus.DRAFT;
        this.updatedBy = actorId;
    }

    public void markDeleted(UUID actorId) {
        this.deletedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public boolean isLateWindowOpen(OffsetDateTime now) {
        return allowLate && lateCloseDate != null && !now.isAfter(lateCloseDate);
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

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public boolean isAllowLate() {
        return allowLate;
    }

    public OffsetDateTime getLateCloseDate() {
        return lateCloseDate;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
