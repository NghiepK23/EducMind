package com.educmind.analytics.dto;

import com.educmind.analytics.entity.KnowledgeGap;

import java.time.OffsetDateTime;
import java.util.UUID;

public class KnowledgeGapResponse {

    private final UUID id;
    private final UUID studentId;
    private final UUID lessonId;
    private final UUID learningObjectiveId;
    private final UUID courseId;
    private final String status;
    private final OffsetDateTime openedAt;
    private final String openedReason;
    private final OffsetDateTime resolvedAt;
    private final String resolvedReason;
    private final UUID dismissedBy;
    private final String dismissedReason;

    public KnowledgeGapResponse(KnowledgeGap g) {
        this.id = g.getId();
        this.studentId = g.getStudentId();
        this.lessonId = g.getLessonId();
        this.learningObjectiveId = g.getLearningObjectiveId();
        this.courseId = g.getCourseId();
        this.status = g.getStatus();
        this.openedAt = g.getOpenedAt();
        this.openedReason = g.getOpenedReason();
        this.resolvedAt = g.getResolvedAt();
        this.resolvedReason = g.getResolvedReason();
        this.dismissedBy = g.getDismissedBy();
        this.dismissedReason = g.getDismissedReason();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getLessonId() { return lessonId; }
    public UUID getLearningObjectiveId() { return learningObjectiveId; }
    public UUID getCourseId() { return courseId; }
    public String getStatus() { return status; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public String getOpenedReason() { return openedReason; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public String getResolvedReason() { return resolvedReason; }
    public UUID getDismissedBy() { return dismissedBy; }
    public String getDismissedReason() { return dismissedReason; }
}
