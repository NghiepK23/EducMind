package com.educmind.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `recommendation` — V7__module_analytics_tables.sql,
 * Module05_LearningEvidence_Analytics_FINAL.md mục 3.4 / 4.5. Không có cột priority.
 */
@Entity
@Table(name = "recommendation")
public class Recommendation {

    public static final String TYPE_REVIEW_CONTENT = "REVIEW_CONTENT";
    public static final String TYPE_PRACTICE_TARGETED = "PRACTICE_TARGETED";
    public static final String TYPE_ADVANCED_ACTIVITY = "ADVANCED_ACTIVITY";
    public static final String TYPE_REATTEMPT_ASSESSMENT = "REATTEMPT_ASSESSMENT";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DISMISSED = "DISMISSED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    public static final String GENERATED_BY_RULE_ENGINE = "RULE_ENGINE";
    public static final String REVIEW_NOT_REQUIRED = "NOT_REQUIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "learning_objective_id", nullable = false)
    private UUID learningObjectiveId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "knowledge_gap_id")
    private UUID knowledgeGapId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "target_resource_id")
    private UUID targetResourceId;

    @Column(name = "generated_by", nullable = false, length = 20)
    private String generatedBy = GENERATED_BY_RULE_ENGINE;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = REVIEW_NOT_REQUIRED;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Recommendation() {
    }

    public Recommendation(UUID studentId, UUID lessonId, UUID learningObjectiveId, UUID courseId,
                          UUID knowledgeGapId, String type, UUID targetResourceId) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.learningObjectiveId = learningObjectiveId;
        this.courseId = courseId;
        this.knowledgeGapId = knowledgeGapId;
        this.type = type;
        this.targetResourceId = targetResourceId;
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getLessonId() { return lessonId; }
    public UUID getLearningObjectiveId() { return learningObjectiveId; }
    public UUID getCourseId() { return courseId; }
    public UUID getKnowledgeGapId() { return knowledgeGapId; }
    public String getType() { return type; }
    public UUID getTargetResourceId() { return targetResourceId; }
    public String getGeneratedBy() { return generatedBy; }
    public String getReviewStatus() { return reviewStatus; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
