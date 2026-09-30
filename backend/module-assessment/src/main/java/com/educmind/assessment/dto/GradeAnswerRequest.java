package com.educmind.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class GradeAnswerRequest {

    @NotNull(message = "questionId không được để trống")
    private UUID questionId;

    private BigDecimal scoreAwarded;

    private String teacherFeedback;

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public BigDecimal getScoreAwarded() {
        return scoreAwarded;
    }

    public void setScoreAwarded(BigDecimal scoreAwarded) {
        this.scoreAwarded = scoreAwarded;
    }

    public String getTeacherFeedback() {
        return teacherFeedback;
    }

    public void setTeacherFeedback(String teacherFeedback) {
        this.teacherFeedback = teacherFeedback;
    }
}
