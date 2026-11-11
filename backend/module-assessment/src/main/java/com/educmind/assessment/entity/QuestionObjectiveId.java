package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionObjectiveId implements Serializable {

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "objective_id")
    private UUID objectiveId;

    protected QuestionObjectiveId() {
    }

    public QuestionObjectiveId(UUID questionId, UUID objectiveId) {
        this.questionId = questionId;
        this.objectiveId = objectiveId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QuestionObjectiveId that)) {
            return false;
        }
        return Objects.equals(questionId, that.questionId) && Objects.equals(objectiveId, that.objectiveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, objectiveId);
    }
}
