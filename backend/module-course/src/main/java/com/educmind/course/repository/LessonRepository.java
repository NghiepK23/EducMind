package com.educmind.course.repository;

import com.educmind.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findByIdAndDeletedAtIsNull(UUID id);

    List<Lesson> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID courseId);

    List<Lesson> findByTopicIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID topicId);

    long countByTopicIdAndDeletedAtIsNull(UUID topicId);
}
