package com.educmind.analytics.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** US-S06: gợi ý học tập cá nhân hóa (có lý do + liên kết nội dung). */
public record RecommendationRow(UUID id, String type, String reason,
                                UUID objectiveId, String objectiveCode, String objectiveDescription,
                                UUID lessonId, String lessonTitle, boolean lessonAvailable,
                                UUID topicId, String topicTitle,
                                String targetEntityType, UUID targetEntityId,
                                UUID relatedAssessmentId, String status,
                                OffsetDateTime createdAt, OffsetDateTime respondedAt) {
}
