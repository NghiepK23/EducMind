package com.educmind.analytics.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** US-T08: nội dung học sinh cần cải thiện. */
public record KnowledgeGapRow(UUID id, UUID studentId, String studentName,
                              UUID objectiveId, String objectiveCode, String objectiveDescription,
                              UUID lessonId, String lessonTitle,
                              UUID topicId, String topicTitle,
                              String status, String reason, BigDecimal masteryScore,
                              OffsetDateTime openedAt, OffsetDateTime resolvedAt,
                              OffsetDateTime dismissedAt, String dismissReason) {
}
