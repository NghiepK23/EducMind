package com.educmind.assignment.repository;

import com.educmind.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    Optional<Assignment> findByIdAndDeletedAtIsNull(UUID id);

    /** TEACHER/SCHOOL_ADMIN xem — mọi publish_status. D.6 GET /courses/{id}/assignments. */
    List<Assignment> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID courseId);

    /** STUDENT xem — chỉ PUBLISHED (đã enroll course, xem D.5 mục 1). */
    List<Assignment> findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID courseId, String publishStatus);

    /** Module 10: assignment PUBLISHED chưa xóa, dueDate trong (from, to]. */
    @Query("SELECT a FROM Assignment a WHERE a.publishStatus = 'PUBLISHED' "
            + "AND a.deletedAt IS NULL AND a.dueDate IS NOT NULL "
            + "AND a.dueDate > :from AND a.dueDate <= :to")
    List<Assignment> findPublishedDueBetween(@Param("from") OffsetDateTime from,
                                             @Param("to") OffsetDateTime to);
}
