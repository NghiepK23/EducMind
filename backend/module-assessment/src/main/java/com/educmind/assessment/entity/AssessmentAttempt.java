package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Bảng assessment_attempts (Spec v2 — bảng 16; US-S03, US-T14, US-T15). */
@Entity
@Table(name = "assessment_attempts")
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "total_score", precision = 6, scale = 2)
    private BigDecimal totalScore;

    protected AssessmentAttempt() {
    }

    public AssessmentAttempt(UUID assessmentId, UUID studentId, int attemptNumber) {
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.attemptNumber = attemptNumber;
        this.status = AttemptStatus.IN_PROGRESS;
    }

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }

    /** Hạn làm bài được chốt tại thời điểm bắt đầu (AC-T11.5). */
    public OffsetDateTime deadline(int durationMinutes) {
        return startedAt.plusMinutes(durationMinutes);
    }

    public void markSubmitted(OffsetDateTime at) {
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = at;
    }

    public void markGraded(BigDecimal totalScore, OffsetDateTime at) {
        this.status = AttemptStatus.GRADED;
        this.totalScore = totalScore;
        this.gradedAt = at;
    }

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

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }
}
