package com.educmind.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Phát khi attempt chuyển sang GRADED (đúng 1 lần cho mỗi attempt). */
public record AssessmentGradedEvent(UUID attemptId, UUID assessmentId, UUID courseId,
                                    UUID studentId, BigDecimal totalScore) {
}
