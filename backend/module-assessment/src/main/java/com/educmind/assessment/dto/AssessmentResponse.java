package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** AC-T06.4: maxScore = tổng trọng số các câu hỏi. */
public record AssessmentResponse(UUID id, UUID courseId, UUID topicId, UUID lessonId, String title,
                                 int durationMinutes, int maxAttempts, OffsetDateTime openAt,
                                 OffsetDateTime closeAt, String status, BigDecimal maxScore,
                                 long questionCount, OffsetDateTime createdAt) {
}
