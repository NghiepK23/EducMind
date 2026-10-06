package com.educmind.assignment.repository;

import com.educmind.assignment.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findById(UUID id);

    /** Check đã nộp chưa trước khi cho phép submit — D.7 ALREADY_SUBMITTED. */
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    /** TEACHER xem tất cả bài nộp — D.6 GET /assignments/{id}/submissions. */
    List<AssignmentSubmission> findByAssignmentId(UUID assignmentId);

    /** Module 10: học sinh đã nộp (SUBMITTED hoặc LATE). */
    @Query("SELECT s.studentId FROM AssignmentSubmission s "
            + "WHERE s.assignmentId = :aid AND s.status IN ('SUBMITTED', 'LATE')")
    List<UUID> findSubmittedStudentIds(@Param("aid") UUID aid);
}
