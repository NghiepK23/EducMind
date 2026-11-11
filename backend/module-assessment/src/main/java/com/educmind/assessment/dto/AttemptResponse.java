package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AttemptResponse(UUID attemptId, UUID assessmentId, String assessmentTitle,
                              int attemptNumber, String status, OffsetDateTime startedAt,
                              OffsetDateTime deadline, OffsetDateTime serverNow,
                              BigDecimal maxScore, BigDecimal totalScore, boolean canSubmit,
                              List<AttemptQuestionView> questions) {
}
