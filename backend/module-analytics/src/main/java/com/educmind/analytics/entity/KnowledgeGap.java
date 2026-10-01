package com.educmind.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `knowledge_gap` — V7__module_analytics_tables.sql,
 * Module05_LearningEvidence_Analytics_FINAL.md mục 3.3 / 4.4.
 *
 * Khóa objective là (lessonId, learningObjectiveId). Tối đa 1 gap OPEN cho mỗi
 * (student, lesson, objective) — ràng buộc bằng partial unique index ở DB.
 * Vòng đời: OPEN -> RESOLVED (mastery đủ cao) hoặc OPEN -> DISMISSED (giáo viên bỏ qua).
 * Sau RESOLVED/DISMISSED, nếu mastery lại thấp thì có thể mở gap MỚI (dòng mới).
 */
@Entity
@Table(name = "knowledge_gap")
public class KnowledgeGap {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_DISMISSED = "DISMISSED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "learning_objective_id", nullable = false)
    private UUID learningObjectiveId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false, length = 20)
    private String status = STATUS_OPEN;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "opened_reason", nullable = false, length = 500)
    private String openedReason;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_reason", length = 500)
    private String resolvedReason;

    @Column(name = "dismissed_by")
    private UUID dismissedBy;

    @Column(name = "dismissed_reason", columnDefinition = "TEXT")
    private String dismissedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected KnowledgeGap() {
    }

    public KnowledgeGap(UUID studentId, UUID lessonId, UUID learningObjectiveId,
                        UUID courseId, String openedReason) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.learningObjectiveId = learningObjectiveId;
        this.courseId = courseId;
        this.openedReason = openedReason;
        OffsetDateTime now = OffsetDateTime.now();
        this.openedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Đóng tự động khi mastery đạt ngưỡng resolve. Chỉ hợp lệ khi gap đang OPEN. */
    public void resolve(String reason) {
        requireOpen();
        this.status = STATUS_RESOLVED;
        this.resolvedReason = reason;
        OffsetDateTime now = OffsetDateTime.now();
        this.resolvedAt = now;
        this.updatedAt = now;
    }

    /** Giáo viên bỏ qua gap. Chỉ hợp lệ khi gap đang OPEN. */
    public void dismiss(UUID teacherId, String reason) {
        requireOpen();
        this.status = STATUS_DISMISSED;
        this.dismissedBy = teacherId;
        this.dismissedReason = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("Knowledge gap không ở trạng thái OPEN: " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public UUID getLearningObjectiveId() {
        return learningObjectiveId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public String getOpenedReason() {
        return openedReason;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolvedReason() {
        return resolvedReason;
    }

    public UUID getDismissedBy() {
        return dismissedBy;
    }

    public String getDismissedReason() {
        return dismissedReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
