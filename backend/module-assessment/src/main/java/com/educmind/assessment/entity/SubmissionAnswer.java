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
import java.time.OffsetDateTime;
import java.util.UUID;

/** Bảng submission_answers (Spec v2 — bảng 17; US-S03, US-T15). */
@Entity
@Table(name = "submission_answers")
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    /** JSONB: mảng chỉ số phương án (trắc nghiệm) hoặc chuỗi (tự luận/ngắn). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_content", columnDefinition = "jsonb")
    private String answerContent;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "text")
    private String feedback;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    protected SubmissionAnswer() {
    }

    public SubmissionAnswer(UUID attemptId, UUID questionId, String answerContent) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.answerContent = answerContent;
    }

    public void updateContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public void autoGrade(BigDecimal score) {
        this.score = score;
        this.gradedBy = null;
        this.gradedAt = OffsetDateTime.now();
    }

    public void manualGrade(BigDecimal score, String feedback, UUID teacherId, OffsetDateTime at) {
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = teacherId;
        this.gradedAt = at;
    }

    public boolean isGraded() {
        return score != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public String getAnswerContent() {
        return answerContent;
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
