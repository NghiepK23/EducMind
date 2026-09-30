package com.educmind.assessment.repository;

import com.educmind.assessment.entity.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho {@link SubmissionAnswer} — bảng `submission_answers`.
 * Module04_Assignment_Assessment_FINAL.md mục E.4/E.5/E.6.
 */
public interface SubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, UUID> {

    /** Toàn bộ câu trả lời của 1 attempt — tính total_score, hiển thị bài làm (E.6 GET /attempts/{id}). */
    List<SubmissionAnswer> findByAttemptId(UUID attemptId);

    /** Check tồn tại trước khi lưu (autosave) hoặc để chấm tay 1 câu cụ thể — UNIQUE(attempt_id, question_id). */
    Optional<SubmissionAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    /**
     * Đếm số câu chưa chấm (is_correct IS NULL) — gồm cả câu tự luận đang chờ chấm tay
     * và (về lý thuyết) câu trắc nghiệm chưa auto-score. Dùng để biết khi nào toàn bộ
     * đã chấm xong để chuyển assessment_attempts.status = GRADED (E.5 mục 4).
     */
    long countByAttemptIdAndIsCorrectIsNull(UUID attemptId);

    /**
     * Đếm số câu CHƯA CÓ ĐIỂM (score_awarded IS NULL) — đây mới là tín hiệu đúng cho
     * "chưa chấm xong", áp dụng cho cả trắc nghiệm (có điểm ngay khi auto-score) lẫn
     * tự luận (có điểm sau khi giáo viên chấm tay). Không dùng is_correct vì câu tự luận
     * luôn có is_correct=NULL kể cả sau khi đã chấm điểm xong.
     */
    long countByAttemptIdAndScoreAwardedIsNull(UUID attemptId);
}
