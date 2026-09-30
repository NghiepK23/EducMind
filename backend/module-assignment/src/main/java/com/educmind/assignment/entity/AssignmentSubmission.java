package com.educmind.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `assignment_submissions` — V4__module_assignment_assessment.sql,
 * Module04_Assignment_Assessment_FINAL.md mục D.4/D.5.
 *
 * {@link #assignmentId} là reference cùng module (assignments). {@link #studentId},
 * {@link #gradedBy} là cross-module reference sang module-auth (bảng users).
 * {@link #resourceId} là cross-module reference sang module-course (bảng resources).
 *
 * [DECIDED — D.5 mục 3] Học sinh chỉ được nộp bài đúng 1 lần, không được sửa/nộp
 * lại kể cả trước deadline (UNIQUE(assignment_id, student_id) ở tầng DB) — vì vậy
 * {@link #submit(String, UUID, boolean)} chỉ được gọi đúng 1 lần trong vòng đời
 * của 1 dòng submission; gọi lại sau khi đã SUBMITTED/LATE là lỗi nghiệp vụ, phải
 * chặn ở tầng Service (ALREADY_SUBMITTED) trước khi gọi entity.
 */
@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, length = 20)
    private String status = "NOT_STARTED";

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    protected AssignmentSubmission() {
    }

    public AssignmentSubmission(UUID assignmentId, UUID studentId) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
    }

    /**
     * Nộp bài — D.5 mục 2/3. Chỉ gọi được đúng 1 lần (Service phải check
     * status == NOT_STARTED trước khi gọi, nếu không throw ALREADY_SUBMITTED).
     *
     * @param isLate true nếu nộp sau due_date và allow_late_submission=true
     *               (Service quyết định true/false dựa trên assignment.dueDate,
     *               entity này không tự biết deadline của assignment cha).
     */
    public void submit(String textAnswer, UUID resourceId, boolean isLate) {
        this.textAnswer = textAnswer;
        this.resourceId = resourceId;
        this.status = isLate ? "LATE" : "SUBMITTED";
        this.submittedAt = OffsetDateTime.now();
    }

    /** Chấm điểm/nhận xét — D.5 mục 5, tùy chọn, không bắt buộc mọi assignment. */
    public void grade(BigDecimal score, String feedback, UUID gradedBy) {
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = gradedBy;
        this.gradedAt = OffsetDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }

    public UUID getGradedBy() {
        return gradedBy;
    }

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }
}
