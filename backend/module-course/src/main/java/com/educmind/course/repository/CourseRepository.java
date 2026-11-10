package com.educmind.course.repository;

import com.educmind.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

    List<Course> findByClassIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID classId);
}
