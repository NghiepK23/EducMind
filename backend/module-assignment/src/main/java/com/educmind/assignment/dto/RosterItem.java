package com.educmind.assignment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** AC-T04.1: danh sách đầy đủ học sinh của lớp, kể cả em chưa nộp bài. */
public record RosterItem(UUID studentId, String fullName, String email, boolean submitted,
                         UUID submissionId, String status, BigDecimal score, boolean pendingGrade,
                         OffsetDateTime submittedAt, boolean edited) {
}
