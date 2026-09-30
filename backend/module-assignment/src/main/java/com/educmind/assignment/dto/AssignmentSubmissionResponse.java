package com.educmind.assignment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response dùng chung cho submit/list/get-me/grade — AssignmentController. */
public class AssignmentSubmissionResponse {

    private final UUID id;
    private final UUID assignmentId;
    private final UUID studentId;
    private final String textAnswer;
    private final UUID resourceId;
    private final String status;
    private final OffsetDateTime submittedAt;
    private final BigDecimal score;
    private final String feedback;
    private final UUID gradedBy;
    private final OffsetDateTime gradedAt;

    public AssignmentSubmissionResponse(UUID id, UUID assignmentId, UUID studentId, String textAnswer,
                                         UUID resourceId, String status, OffsetDateTime submittedAt,
                                         BigDecimal score, String feedback, UUID gradedBy,
                                         OffsetDateTime gradedAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.textAnswer = textAnswer;
        this.resourceId = resourceId;
        this.status = status;
        this.submittedAt = submittedAt;
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = gradedBy;
        this.gradedAt = gradedAt;
    }

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
