package com.educmind.course.repository;

import com.educmind.course.entity.CourseTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTopicRepository extends JpaRepository<CourseTopic, UUID> {

    Optional<CourseTopic> findByIdAndDeletedAtIsNull(UUID id);

    List<CourseTopic> findByCourseIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(UUID courseId);

    boolean existsByCourseIdAndDeletedAtIsNull(UUID courseId);
}
