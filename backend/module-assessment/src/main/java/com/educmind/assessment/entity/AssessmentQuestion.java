package com.educmind.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity ứng với bảng `assessment_questions` — V4__module_assignment_assessment.sql,
 * Module04_Assignment_Assessment_FINAL.md mục E.4. Bảng nối (assessment_id, question_id)
 * — same-module reference tới cả 2 entity còn lại trong module này, dùng raw UUID
 * (không @ManyToOne) để nhất quán với convention cross-module, dù ở đây cùng module —
 * tránh phải quyết định 2 quy tắc khác nhau cho "cùng module" vs "khác module".
 *
 * UNIQUE(assessment_id, question_id) ở tầng DB — Service phải chặn trùng lặp trước khi
 * insert (ASSESSMENT_LOCKED nếu assessment đã có attempt, theo E.5 mục 6).
 */
@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "score_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal scoreWeight = BigDecimal.ONE;

    protected AssessmentQuestion() {
    }

    public AssessmentQuestion(UUID assessmentId, UUID questionId) {
        this.assessmentId = assessmentId;
        this.questionId = questionId;
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

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

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public BigDecimal getScoreWeight() {
        return scoreWeight;
    }

    public void setScoreWeight(BigDecimal scoreWeight) {
        this.scoreWeight = scoreWeight;
    }
}
