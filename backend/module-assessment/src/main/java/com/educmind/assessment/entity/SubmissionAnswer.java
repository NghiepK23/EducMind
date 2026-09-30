package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity ứng với bảng `submission_answers` — V4__module_assignment_assessment.sql,
 * Module04_Assignment_Assessment_FINAL.md mục E.4/E.5.
 *
 * {@link #attemptId} là same-module reference (assessment_attempts.id).
 * {@link #questionId} là same-module reference (questions.id).
 *
 * KHÔNG có createdAt/updatedAt như các entity khác — bảng gốc không có, giữ đúng
 * theo V4 migration thật (cùng lý do đã ghi ở AssessmentAttempt).
 *
 * {@link #studentAnswer}: JSONB tự do, hình dạng theo question_type (cùng lý do
 * Question.options/correctAnswer) — dùng Object chung.
 *
 * [DECIDED — E.5 mục 4] isCorrect/scoreAwarded = NULL cho câu SHORT_ANSWER/ESSAY
 * (chờ giáo viên chấm tay) — {@link #autoScore(boolean, BigDecimal)} chỉ dùng cho
 * câu trắc nghiệm tự động chấm ngay lúc nộp bài; {@link #gradeManually(BigDecimal, String)}
 * dùng cho giáo viên chấm tay câu tự luận (E.6, PATCH /attempts/{id}/grade).
 */
@Entity
@Table(name = "submission_answers")
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "student_answer", columnDefinition = "jsonb")
    private Object studentAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "score_awarded", precision = 5, scale = 2)
    private BigDecimal scoreAwarded;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    protected SubmissionAnswer() {
    }

    public SubmissionAnswer(UUID attemptId, UUID questionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
    }

    /** Lưu câu trả lời (autosave, có thể gọi nhiều lần trước khi nộp) — E.6, PATCH /attempts/{id}/answers. */
    public void saveAnswer(Object studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    /** Auto-score lúc nộp bài — chỉ cho SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE (E.5 mục 4). */
    public void autoScore(boolean isCorrect, BigDecimal scoreAwarded) {
        this.isCorrect = isCorrect;
        this.scoreAwarded = scoreAwarded;
    }

    /** Chấm tay câu tự luận — SHORT_ANSWER/ESSAY (E.5 mục 4, E.6). */
    public void gradeManually(BigDecimal scoreAwarded, String teacherFeedback) {
        this.scoreAwarded = scoreAwarded;
        this.teacherFeedback = teacherFeedback;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public Object getStudentAnswer() {
        return studentAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public BigDecimal getScoreAwarded() {
        return scoreAwarded;
    }

    public String getTeacherFeedback() {
        return teacherFeedback;
    }
}
