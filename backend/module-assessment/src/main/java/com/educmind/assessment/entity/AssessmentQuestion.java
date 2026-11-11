package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Bảng assessment_questions (Spec v2 — bảng 15; AC-T06.3, AC-T06.4). */
@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    protected AssessmentQuestion() {
    }

    public AssessmentQuestion(UUID assessmentId, UUID questionId, BigDecimal weight) {
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.weight = weight;
    }

    public void updateWeight(BigDecimal weight) {
        this.weight = weight;
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

    public BigDecimal getWeight() {
        return weight;
    }
}
