package com.educmind.analytics.dto;

import com.educmind.analytics.entity.Recommendation;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RecommendationResponse {

    private final UUID id;
    private final UUID lessonId;
    private final UUID learningObjectiveId;
    private final UUID courseId;
    private final UUID knowledgeGapId;
    private final String type;
    private final UUID targetResourceId;
    private final String generatedBy;
    private final String status;
    private final OffsetDateTime createdAt;

    public RecommendationResponse(Recommendation r) {
        this.id = r.getId();
        this.lessonId = r.getLessonId();
        this.learningObjectiveId = r.getLearningObjectiveId();
        this.courseId = r.getCourseId();
        this.knowledgeGapId = r.getKnowledgeGapId();
        this.type = r.getType();
        this.targetResourceId = r.getTargetResourceId();
        this.generatedBy = r.getGeneratedBy();
        this.status = r.getStatus();
        this.createdAt = r.getCreatedAt();
    }

    public UUID getId() { return id; }
    public UUID getLessonId() { return lessonId; }
    public UUID getLearningObjectiveId() { return learningObjectiveId; }
    public UUID getCourseId() { return courseId; }
    public UUID getKnowledgeGapId() { return knowledgeGapId; }
    public String getType() { return type; }
    public UUID getTargetResourceId() { return targetResourceId; }
    public String getGeneratedBy() { return generatedBy; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
