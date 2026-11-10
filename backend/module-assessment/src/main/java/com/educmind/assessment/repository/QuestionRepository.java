package com.educmind.assessment.repository;

import com.educmind.assessment.entity.DifficultyLevel;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Optional<Question> findByIdAndDeletedAtIsNull(UUID id);

    List<Question> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID courseId);

    List<Question> findByCourseIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(UUID courseId, QuestionType type);

    List<Question> findByCourseIdAndDifficultyAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID courseId, DifficultyLevel difficulty);
}
