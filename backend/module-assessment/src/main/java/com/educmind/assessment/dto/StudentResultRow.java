package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** AC-T14.1/14.2/14.3: mọi học sinh của lớp; điểm chính thức = điểm cao nhất trong các lượt đã chấm. */
public record StudentResultRow(UUID studentId, String fullName, String email, long attemptCount,
                               long gradedCount, long pendingCount, BigDecimal bestScore,
                               BigDecimal maxScore) {
}
