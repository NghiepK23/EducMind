package com.educmind.course.repository;

import com.educmind.course.entity.Lesson;
import com.educmind.course.entity.PublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity {@link Lesson} — Module03_Course_Lesson_Resource_FINAL.md, mục C.4/C.5.
 */
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    /**
     * Tìm 1 lesson theo id, loại trừ đã soft-delete — dùng cho
     * GET/PATCH/DELETE /api/v1/lessons/{id}.
     */
    Optional<Lesson> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Toàn bộ lesson (mọi publishStatus) của 1 course, sắp theo orderIndex tăng dần —
     * dùng cho giáo viên xem GET /api/v1/courses/{id}/lessons (thấy cả DRAFT + PUBLISHED).
     */
    List<Lesson> findByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID courseId);

    /**
     * Lesson của 1 course lọc theo publishStatus, sắp theo orderIndex tăng dần —
     * dùng cho học sinh xem cùng endpoint trên (chỉ truyền PublishStatus.PUBLISHED).
     */
    List<Lesson> findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
            UUID courseId, PublishStatus publishStatus);
}
