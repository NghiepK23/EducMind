package com.educmind.course.repository;

import com.educmind.course.entity.ProcessingStatus;
import com.educmind.course.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

    /** Resource mới nhất (chưa xóa) của 1 lesson — dùng chọn target_resource_id cho recommendation. */
    Optional<Resource> findFirstByLessonIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID lessonId);

    /**
     * Đổi sang PROCESSING có điều kiện (nguyên tử, một câu UPDATE): chỉ khi chưa xóa mềm và
     * (chưa PROCESSING hoặc updatedAt cũ hơn staleBefore). Trả số dòng đã đổi (0 hoặc 1).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Resource r SET r.processingStatus = :processing, r.updatedAt = :now "
            + "WHERE r.id = :id AND r.deletedAt IS NULL "
            + "AND (r.processingStatus <> :processing OR r.updatedAt < :staleBefore)")
    int tryStartProcessing(@Param("id") UUID id,
                           @Param("processing") ProcessingStatus processing,
                           @Param("now") OffsetDateTime now,
                           @Param("staleBefore") OffsetDateTime staleBefore);
}
