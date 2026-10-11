package com.educmind.reports.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssessmentResultReport(
        UUID assessmentId,
        OffsetDateTime generatedAt,
        long totalAttempts,
        long gradedAttempts,
        BigDecimal minScore,
        BigDecimal avgScore,
        BigDecimal maxScore,
        List<HardQuestion> hardestQuestions) {

    public record HardQuestion(UUID questionId, long answeredCount, long incorrectCount,
                               double incorrectRate) {
    }
}
