package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Câu hỏi trong lượt làm của học sinh — KHÔNG chứa đáp án đúng.
 * `score`/`feedback` chỉ có giá trị khi lượt làm đã chấm (AC-S04.2, AC-T15.4).
 */
public record AttemptQuestionView(UUID questionId, String content, String type, List<String> options,
                                  BigDecimal weight, List<Integer> selectedOptionIndexes,
                                  String text, BigDecimal score, String feedback) {
}
