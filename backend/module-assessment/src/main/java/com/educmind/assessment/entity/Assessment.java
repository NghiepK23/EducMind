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
 * Entity ứng với bảng `assessments` — V4__module_assignment_assessment.sql,
 * Module04_Assignment_Assessment_FINAL.md mục E.4/E.5.
 *
 * {@link #courseId} là cross-module reference tới module-course (bảng courses).
 * {@link #createdBy} là cross-module reference sang module-auth (bảng users).
 *
 * KHÔNG có assessment_class (đã bỏ theo ghi chú đầu file V4) — opensAt/closesAt
 * là 1 mốc DUY NHẤT dùng chung cho toàn bộ course_members của course, không phải
 * theo từng lớp như spec Module04_FINAL.md mô tả ban đầu.
 *
 * {@link #blueprintConfig}: JSONB, hình dạng tự do theo ma trận đề (vd:
 * [{"objective":"LO02","difficulty":"MEDIUM","count":3}]) — dùng Object chung,
 * cùng lý do đã ghi ở Question.options/correctAnswer.
 *
 * {@link #publishedAt}: giữ nguyên pattern của Lesson.java (module-course) — chỉ
 * set 1 LẦN DUY NHẤT khi publish lần đầu, KHÔNG bị xóa về null khi unpublish.
 */
@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blueprint_config", columnDefinition = "jsonb")
    private Object blueprintConfig;

    @Column(name = "opens_at")
    private OffsetDateTime opensAt;

    @Column(name = "closes_at")
    private OffsetDateTime closesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private AssessmentPublishStatus publishStatus = AssessmentPublishStatus.DRAFT;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Assessment() {
    }

    public Assessment(UUID courseId, String title, UUID createdBy) {
        this.courseId = courseId;
        this.title = title;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * [DECIDED — theo pattern Lesson.publish()] Chuyển sang PUBLISHED. publishedAt
     * chỉ set nếu đang null (lần publish đầu tiên) — publish lại sau khi từng
     * unpublish KHÔNG ghi đè mốc publish gốc.
     */
    public void publish() {
        this.publishStatus = AssessmentPublishStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = OffsetDateTime.now();
        }
    }

    /** Chuyển về DRAFT. publishedAt GIỮ NGUYÊN, không set về null. */
    public void unpublish() {
        this.publishStatus = AssessmentPublishStatus.DRAFT;
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Object getBlueprintConfig() {
        return blueprintConfig;
    }

    public void setBlueprintConfig(Object blueprintConfig) {
        this.blueprintConfig = blueprintConfig;
    }

    public OffsetDateTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(OffsetDateTime opensAt) {
        this.opensAt = opensAt;
    }

    public OffsetDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(OffsetDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public AssessmentPublishStatus getPublishStatus() {
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

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
