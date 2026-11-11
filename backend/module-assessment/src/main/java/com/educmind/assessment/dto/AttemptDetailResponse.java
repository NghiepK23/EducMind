package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AttemptDetailResponse(UUID attemptId, UUID assessmentId, UUID studentId, String studentName,
                                    int attemptNumber, String status, BigDecimal totalScore,
                                    BigDecimal maxScore, OffsetDateTime startedAt,
                                    OffsetDateTime submittedAt, List<AttemptAnswerDetail> answers) {
}
