package com.educmind.assignment.dto;

import java.math.BigDecimal;

public class GradeSubmissionRequest {

    private BigDecimal score;
    private String feedback;

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
