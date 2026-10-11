package com.educmind.common.port;

import java.util.List;
import java.util.UUID;

/**
 * Cổng để Module 7 ghi câu hỏi AI đã được giáo viên duyệt vào ngân hàng câu hỏi (Module 4).
 * Adapter (module-assessment) tự kiểm tra dữ liệu; việc kiểm quyền người gọi do bên gọi chịu trách nhiệm.
 */
public interface QuestionBankPort {

    /**
     * Nháp 1 câu hỏi. Kiểu enum truyền dạng chuỗi để educmind-common không phụ thuộc module-assessment.
     * options / correctAnswer là cấu trúc JSON thô (List/Map).
     */
    record AiQuestionDraft(UUID lessonId, UUID learningObjectiveId, String questionText,
                           String questionType, String difficulty, String cognitiveLevel,
                           Object options, Object correctAnswer, String explanation) {
    }

    /**
     * Tạo các câu hỏi source=AI_GENERATED, review_status=APPROVED (đặt cứng, PATCH 1).
     * Chạy chung transaction của bên gọi; có lỗi thì ném BusinessException và không câu nào được lưu.
     *
     * @return danh sách id câu hỏi đã tạo, đúng thứ tự đầu vào
     */
    List<UUID> createApprovedAiQuestions(UUID actorId, UUID courseId, List<AiQuestionDraft> drafts);
}
