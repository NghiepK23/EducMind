package com.educmind.assessment.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** Bảng question_objectives (Spec v2 — bảng 13; AC-T05.3, AC-T05.5, AC-T05.6). */
@Entity
@Table(name = "question_objectives")
public class QuestionObjective {

    @EmbeddedId
    private QuestionObjectiveId id;

    protected QuestionObjective() {
    }

    public QuestionObjective(UUID questionId, UUID objectiveId) {
        this.id = new QuestionObjectiveId(questionId, objectiveId);
    }

    public QuestionObjectiveId getId() {
        return id;
    }

    public UUID getQuestionId() {
        return id.getQuestionId();
    }

    public UUID getObjectiveId() {
        return id.getObjectiveId();
    }
}
