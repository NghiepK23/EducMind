package com.educmind.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** AC-T07.3: kết quả trung bình của lớp theo mục tiêu, chỉ tính học sinh đã có dữ liệu. */
public record ObjectiveSummaryRow(UUID objectiveId, String code, String description,
                                  UUID lessonId, String lessonTitle,
                                  UUID topicId, String topicTitle,
                                  String status, BigDecimal averageScore,
                                  int studentsWithData, int studentsTotal) {
}
