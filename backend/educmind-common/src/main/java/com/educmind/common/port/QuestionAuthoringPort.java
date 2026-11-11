package com.educmind.common.port;

import java.util.List;
import java.util.UUID;

/**
 * Port để module AI (teacher-assistant) tạo câu hỏi chính thức trong ngân hàng câu hỏi
 * mà không phụ thuộc Maven vào module-assessment (AC-T10.2).
 */
public interface QuestionAuthoringPort {

    /**
     * Tạo câu hỏi nguồn AI_GENERATED gắn với đúng một mục tiêu của bài học.
     * Ném BusinessException nếu dữ liệu không hợp lệ (phương án/đáp án/độ khó/mục tiêu).
     *
     * @return id câu hỏi vừa tạo
     */
    UUID createFromDraft(UUID teacherId, UUID lessonId, UUID objectiveId,
                         String content, String type, String difficulty, String bloomLevel,
                         List<String> options, List<Integer> correctOptionIndexes,
                         String expectedAnswer);
}
