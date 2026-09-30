package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `assessment_attempts` — V4__module_assignment_assessment.sql,
 * Module04_Assignment_Assessment_FINAL.md mục E.4/E.5.
 *
 * {@link #assessmentId} là same-module reference (assessments.id).
 * {@link #studentId} là cross-module reference sang module-auth (bảng users).
 *
 * KHÔNG có createdAt/updatedAt/deletedAt như các entity khác — bảng gốc không có
 * 3 cột này (chỉ startedAt/submittedAt), giữ đúng theo V4 migration thật, không tự
 * thêm audit column không có trong schema.
 *
 * [DECIDED — E.5 mục 4/5] status chỉ chuyển GRADED sau khi TOÀN BỘ câu tự luận
 * (SHORT_ANSWER/ESSAY) trong attempt đã được giáo viên chấm — entity không tự biết
 * điều kiện này (cần join submission_answers), nên {@link #markGraded(BigDecimal)}
 * chỉ là thao tác thuần túy đổi trạng thái + set điểm, Service phải tự kiểm tra đủ
 * điều kiện trước khi gọi.
 */
@Entity
@Table(name = "assessment_attempts")
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "total_score", precision = 6, scale = 2)
    private BigDecimal totalScore;

    protected AssessmentAttempt() {
    }

    public AssessmentAttempt(UUID assessmentId, UUID studentId, int attemptNumber) {
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.attemptNumber = attemptNumber;
        this.startedAt = OffsetDateTime.now();
    }

    /**
     * Nộp bài — E.6, POST /attempts/{id}/submit. Chuyển IN_PROGRESS -> SUBMITTED.
     * Service tự quyết định có auto-score xong hết trắc nghiệm rồi mới gọi
     * {@link #markGraded(BigDecimal)} ngay lập tức hay không (nếu đề không có
     * câu tự luận nào — E.5 mục 4), tùy vào kết quả join submission_answers.
     */
    public void submit() {
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
    }

    /**
     * Đánh dấu đã chấm xong toàn bộ — E.5 mục 4/5. Chỉ gọi sau khi Service xác nhận
     * mọi câu tự luận đã có score_awarded (hoặc đề không có câu tự luận nào).
     */
    public void markGraded(BigDecimal totalScore) {
        this.status = AttemptStatus.GRADED;
        this.totalScore = totalScore;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }
}
