package com.educmind.assessment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Chi tiết từng câu trả lời cho giáo viên chấm tay (US-T15). */
public record AttemptAnswerDetail(UUID questionId, String content, String type, List<String> options,
                                  List<Integer> correctOptionIndexes, String expectedAnswer,
                                  BigDecimal weight, List<Integer> selectedOptionIndexes, String text,
                                  BigDecimal score, String feedback) {
}
