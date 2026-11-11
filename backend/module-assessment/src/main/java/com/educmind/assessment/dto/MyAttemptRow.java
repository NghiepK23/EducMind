package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** AC-S04.2/04.3: điểm chỉ hiển thị với lượt đã chấm xong. */
public record MyAttemptRow(UUID attemptId, int attemptNumber, String status, BigDecimal totalScore,
                           BigDecimal maxScore, OffsetDateTime startedAt, OffsetDateTime submittedAt) {
}
