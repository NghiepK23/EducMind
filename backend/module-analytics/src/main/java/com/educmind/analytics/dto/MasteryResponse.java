package com.educmind.analytics.dto;

import com.educmind.analytics.entity.Mastery;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class MasteryResponse {

    private final UUID id;
    private final UUID lessonId;
    private final UUID learningObjectiveId;
    private final UUID courseId;
    private final BigDecimal masteryScore;
    private final int evidenceCount;
    private final String confidenceLevel;
    private final OffsetDateTime lastEvidenceAt;
    private final OffsetDateTime computedAt;

    public MasteryResponse(Mastery m) {
        this.id = m.getId();
        this.lessonId = m.getLessonId();
        this.learningObjectiveId = m.getLearningObjectiveId();
        this.courseId = m.getCourseId();
        this.masteryScore = m.getMasteryScore();
        this.evidenceCount = m.getEvidenceCount();
        this.confidenceLevel = m.getConfidenceLevel();
        this.lastEvidenceAt = m.getLastEvidenceAt();
        this.computedAt = m.getComputedAt();
    }

    public UUID getId() { return id; }
    public UUID getLessonId() { return lessonId; }
    public UUID getLearningObjectiveId() { return learningObjectiveId; }
    public UUID getCourseId() { return courseId; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public int getEvidenceCount() { return evidenceCount; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public OffsetDateTime getLastEvidenceAt() { return lastEvidenceAt; }
    public OffsetDateTime getComputedAt() { return computedAt; }
}
