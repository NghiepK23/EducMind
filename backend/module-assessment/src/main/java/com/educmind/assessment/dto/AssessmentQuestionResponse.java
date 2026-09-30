package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class AssessmentQuestionResponse {

    private final UUID id;
    private final UUID assessmentId;
    private final UUID questionId;
    private final int orderIndex;
    private final BigDecimal scoreWeight;

    public AssessmentQuestionResponse(UUID id, UUID assessmentId, UUID questionId, int orderIndex,
                                       BigDecimal scoreWeight) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.orderIndex = orderIndex;
        this.scoreWeight = scoreWeight;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public BigDecimal getScoreWeight() {
        return scoreWeight;
    }
}
