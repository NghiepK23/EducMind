package com.educmind.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Phát khi bài nộp assignment được chấm và có điểm. */
public record AssignmentGradedEvent(UUID submissionId, UUID assignmentId, UUID courseId,
                                    UUID studentId, String assignmentTitle,
                                    BigDecimal score, BigDecimal maxScore) {
}
