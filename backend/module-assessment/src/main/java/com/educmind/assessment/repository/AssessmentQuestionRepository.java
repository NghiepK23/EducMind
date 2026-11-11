package com.educmind.assessment.repository;

import com.educmind.assessment.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    List<AssessmentQuestion> findByAssessmentIdOrderByIdAsc(UUID assessmentId);

    void deleteByAssessmentId(UUID assessmentId);

    boolean existsByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    List<AssessmentQuestion> findByQuestionId(UUID questionId);

    long countByAssessmentId(UUID assessmentId);
}
