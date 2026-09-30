package com.educmind.assignment.repository;

import com.educmind.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
