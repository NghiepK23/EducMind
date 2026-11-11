package com.educmind.assessment.repository;

import com.educmind.assessment.entity.AssessmentAttempt;
import com.educmind.assessment.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    Optional<AssessmentAttempt> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<AssessmentAttempt> findByAssessmentIdAndStudentIdAndStatus(
            UUID assessmentId, UUID studentId, AttemptStatus status);

    List<AssessmentAttempt> findByAssessmentIdAndStudentIdOrderByAttemptNumberAsc(
            UUID assessmentId, UUID studentId);

    List<AssessmentAttempt> findByAssessmentIdAndStudentIdInOrderByStartedAtAsc(
            UUID assessmentId, List<UUID> studentIds);

    List<AssessmentAttempt> findByAssessmentIdOrderByStartedAtAsc(UUID assessmentId);

    List<AssessmentAttempt> findByAssessmentIdAndStatusOrderByStartedAtAsc(
            UUID assessmentId, AttemptStatus status);

    long countByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    long countByAssessmentId(UUID assessmentId);

    long countByAssessmentIdAndStatus(UUID assessmentId, AttemptStatus status);
}
