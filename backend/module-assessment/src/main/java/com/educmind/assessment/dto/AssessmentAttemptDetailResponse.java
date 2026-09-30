package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dùng riêng cho GET /api/v1/attempts/{id} — giống AssessmentAttemptResponse
 * nhưng kèm thêm danh sách answers[] (E.7: mẫu response có auto_scored_count/
 * pending_manual_grading; ở đây trả nguyên answers[] để FE tự suy ra 2 số đó
 * và hiển thị isCorrect/scoreAwarded từng câu).
 */
public class AssessmentAttemptDetailResponse {

    private final UUID id;
    private final UUID assessmentId;
    private final UUID studentId;
    private final int attemptNumber;
    private final String status;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime submittedAt;
    private final BigDecimal totalScore;
    private final List<SubmissionAnswerResponse> answers;

    public AssessmentAttemptDetailResponse(UUID id, UUID assessmentId, UUID studentId, int attemptNumber,
                                            String status, OffsetDateTime startedAt, OffsetDateTime submittedAt,
                                            BigDecimal totalScore, List<SubmissionAnswerResponse> answers) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.totalScore = totalScore;
        this.answers = answers;
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

    public List<SubmissionAnswerResponse> getAnswers() {
        return answers;
    }
}
