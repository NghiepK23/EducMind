package com.educmind.assessment.repository;

import com.educmind.assessment.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    /** Danh sách câu hỏi đã thêm vào 1 đề, theo đúng thứ tự hiển thị. */
    List<AssessmentQuestion> findByAssessmentIdOrderByOrderIndexAsc(UUID assessmentId);

    /** Check trùng lặp trước khi thêm câu hỏi vào đề (UNIQUE(assessment_id, question_id)). */
    Optional<AssessmentQuestion> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    /** Dùng khi kiểm tra đề đã có câu hỏi nào chưa (đề rỗng không publish được). */
    long countByAssessmentId(UUID assessmentId);
}
