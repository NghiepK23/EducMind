package com.educmind.assessment.repository;

import com.educmind.assessment.entity.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, UUID> {

    List<SubmissionAnswer> findByAttemptIdOrderByQuestionIdAsc(UUID attemptId);

    Optional<SubmissionAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    long countByAttemptId(UUID attemptId);
}
