package com.educmind.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Chi tiết một mục tiêu kèm các kết quả đánh giá (AC-T07.4, AC-S05.2). */
public record ObjectiveDetailRow(UUID objectiveId, String code, String description,
                                 UUID lessonId, String lessonTitle, boolean lessonAvailable,
                                 UUID topicId, String topicTitle,
                                 String status, BigDecimal masteryScore, int evidenceCount,
                                 List<EvidenceRow> evidences) {
}
