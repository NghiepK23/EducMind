package com.educmind.course.repository;

import com.educmind.course.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity {@link Resource} — Module03_Course_Lesson_Resource_FINAL.md, mục C.4/C.5.
 */
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    /**
     * Tìm 1 resource theo id, loại trừ đã soft-delete — dùng cho
     * GET/DELETE /api/v1/resources/{id} và GET /api/v1/resources/{id}/download.
     */
    Optional<Resource> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Resource gắn cấp course (tài liệu chung, lessonId = NULL) —
     * dùng khi liệt kê tài liệu chung của 1 course.
     */
    List<Resource> findByCourseIdAndLessonIdIsNullAndDeletedAtIsNull(UUID courseId);

    /**
     * Resource gắn cấp 1 lesson cụ thể — dùng khi liệt kê tài liệu của 1 lesson.
     */
    List<Resource> findByLessonIdAndDeletedAtIsNull(UUID lessonId);
}
