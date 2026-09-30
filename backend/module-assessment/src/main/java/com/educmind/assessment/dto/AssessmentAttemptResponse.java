package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AssessmentAttemptResponse {

    private final UUID id;
    private final UUID assessmentId;
    private final UUID studentId;
    private final int attemptNumber;
    private final String status;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime submittedAt;
    private final BigDecimal totalScore;

    public AssessmentAttemptResponse(UUID id, UUID assessmentId, UUID studentId, int attemptNumber,
                                      String status, OffsetDateTime startedAt, OffsetDateTime submittedAt,
                                      BigDecimal totalScore) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.totalScore = totalScore;
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

    public String getStatus() {
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
