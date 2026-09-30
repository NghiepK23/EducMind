package com.educmind.assessment.repository;

import com.educmind.assessment.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    Optional<AssessmentAttempt> findById(UUID id);

    /** Đếm số lần học sinh đã làm bài — dùng để check `attemptNumber < max_attempts`. */
    long countByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    /** Lấy attempt mới nhất của học sinh cho 1 đề (dùng để tính attemptNumber tiếp theo). */
    Optional<AssessmentAttempt> findTopByAssessmentIdAndStudentIdOrderByAttemptNumberDesc(
            UUID assessmentId, UUID studentId);

    /** Danh sách bài làm của học sinh cho 1 đề (xem lịch sử các lần làm). */
    List<AssessmentAttempt> findByAssessmentIdAndStudentIdOrderByAttemptNumberDesc(
            UUID assessmentId, UUID studentId);

    /** TEACHER xem toàn bộ bài nộp của 1 đề — E.6 GET /assessments/{id}/submissions (nếu có). */
    List<AssessmentAttempt> findByAssessmentIdOrderBySubmittedAtDesc(UUID assessmentId);

    /** Guardrail Module 8 H.5.4: check học sinh có attempt IN_PROGRESS cho course nào không. */
    List<AssessmentAttempt> findByStudentIdAndStatus(UUID studentId, com.educmind.assessment.entity.AttemptStatus status);

    /** Check đề đã có học sinh làm bài chưa — chặn sửa danh sách câu hỏi (E.5 mục 6, ASSESSMENT_LOCKED). */
    boolean existsByAssessmentId(UUID assessmentId);
}
