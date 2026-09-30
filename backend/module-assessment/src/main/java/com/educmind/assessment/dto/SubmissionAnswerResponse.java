package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class SubmissionAnswerResponse {

    private final UUID id;
    private final UUID attemptId;
    private final UUID questionId;
    private final Object studentAnswer;
    private final Boolean isCorrect;
    private final BigDecimal scoreAwarded;
    private final String teacherFeedback;

    public SubmissionAnswerResponse(UUID id, UUID attemptId, UUID questionId, Object studentAnswer,
                                     Boolean isCorrect, BigDecimal scoreAwarded, String teacherFeedback) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.studentAnswer = studentAnswer;
        this.isCorrect = isCorrect;
        this.scoreAwarded = scoreAwarded;
        this.teacherFeedback = teacherFeedback;
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
