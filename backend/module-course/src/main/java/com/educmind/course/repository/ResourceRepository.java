package com.educmind.course.repository;

import com.educmind.course.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findByIdAndDeletedAtIsNull(UUID id);

    List<Resource> findByLessonIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID lessonId);

    List<Resource> findByCourseIdAndLessonIdIsNullAndDeletedAtIsNullOrderByUploadedAtDesc(UUID courseId);
}
