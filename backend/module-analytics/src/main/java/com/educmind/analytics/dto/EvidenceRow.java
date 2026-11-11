package com.educmind.analytics.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một kết quả đánh giá tạo nên mastery (AC-T07.4).
 * `targetType`: ASSIGNMENT_SUBMISSION | ASSESSMENT; `targetApi` để giáo viên mở thẳng
 * bài nộp / danh sách lượt làm tương ứng.
 */
public record EvidenceRow(String sourceType, UUID sourceId, BigDecimal score,
                          OffsetDateTime updatedAt, String targetType, String targetApi) {
}
