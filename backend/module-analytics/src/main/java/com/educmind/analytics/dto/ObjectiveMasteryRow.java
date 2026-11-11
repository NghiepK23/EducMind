package com.educmind.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Một mục tiêu trong bảng mastery của lớp (AC-T07.1/02). */
public record ObjectiveMasteryRow(UUID objectiveId, String code, String description,
                                  UUID lessonId, String lessonTitle, boolean lessonAvailable,
                                  UUID topicId, String topicTitle,
                                  String status, BigDecimal masteryScore, int evidenceCount) {
}
