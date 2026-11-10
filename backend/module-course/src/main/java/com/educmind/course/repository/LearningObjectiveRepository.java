package com.educmind.course.repository;

import com.educmind.course.entity.LearningObjective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningObjectiveRepository extends JpaRepository<LearningObjective, UUID> {

    List<LearningObjective> findByLessonIdOrderByCodeAsc(UUID lessonId);

    Optional<LearningObjective> findByIdAndLessonId(UUID id, UUID lessonId);

    boolean existsByLessonIdAndCodeIgnoreCase(UUID lessonId, String code);

    long countByLessonId(UUID lessonId);
}
