package com.educmind.common.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Port cho Module 10 (notification) truy vấn assignment sắp đến hạn mà không
 * phụ thuộc trực tiếp vào module-assignment.
 */
public interface AssignmentDeadlinePort {

    /** Assignment PUBLISHED, chưa xóa, có dueDate trong khoảng (from, to]. */
    List<DueAssignment> findPublishedDueBetween(OffsetDateTime from, OffsetDateTime to);

    /** Học sinh đã nộp (status SUBMITTED hoặc LATE) assignment này. */
    Set<UUID> findSubmittedStudentIds(UUID assignmentId);

    record DueAssignment(UUID assignmentId, UUID courseId, String title, OffsetDateTime dueDate) {
    }
}
