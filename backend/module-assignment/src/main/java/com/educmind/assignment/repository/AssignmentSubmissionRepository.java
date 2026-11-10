package com.educmind.assignment.repository;

import com.educmind.assignment.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByIdAndDeletedAtIsNull(UUID id);

    /** Bài nộp đang hiệu lực (chưa xoá mềm). */
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentIdAndDeletedAtIsNull(UUID assignmentId, UUID studentId);

    /** Kể cả đã xoá mềm — cần để khôi phục khi học sinh nộp lại (AC-S07.4). */
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    List<AssignmentSubmission> findByAssignmentIdAndDeletedAtIsNullOrderBySubmittedAtAsc(UUID assignmentId);

    List<AssignmentSubmission> findByStudentIdAndDeletedAtIsNullOrderBySubmittedAtDesc(UUID studentId);

    boolean existsByAssignmentIdAndDeletedAtIsNull(UUID assignmentId);

    long countByAssignmentIdAndGradedAtIsNotNullAndDeletedAtIsNull(UUID assignmentId);
}
