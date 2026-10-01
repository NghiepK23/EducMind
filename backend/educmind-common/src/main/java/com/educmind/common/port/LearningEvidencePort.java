package com.educmind.common.port;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Port cho phép module-assignment và module-assessment báo cho module-analytics
 * (Module 5) biết có learning evidence mới — không phụ thuộc Maven trực tiếp vào
 * module-analytics. Gọi đồng bộ, theo Module05_LearningEvidence_Analytics_FINAL.md
 * mục 4.1 / 4.2.
 *
 * Khóa objective xuyên suốt là (lessonId, learningObjectiveId) — xem Checkpoint #14.
 * Implementation phải idempotent theo (sourceType, sourceId): gọi lặp không tạo trùng.
 * Implementation tự xác định class_id qua CourseAccessPort (source_class_id).
 */
public interface LearningEvidencePort {

    /**
     * Điểm gộp của 1 nhóm (lessonId, learningObjectiveId) trong 1 attempt.
     * lessonId và learningObjectiveId cùng null = nhóm "chung" (câu hỏi không gắn lesson).
     * totalAwarded, totalWeight cùng thang weight của câu hỏi, không phải tỉ lệ 0-1.
     */
    record ObjectiveGroupScore(UUID lessonId, UUID learningObjectiveId,
                               BigDecimal totalAwarded, BigDecimal totalWeight) {
    }

    /** Học sinh nộp assignment (SUBMITTED/LATE): tạo evidence score = 1.00. */
    void onAssignmentSubmitted(UUID courseId, UUID studentId, UUID submissionId,
                               UUID lessonId, OffsetDateTime occurredAt);

    /** Giáo viên chấm assignment: cập nhật score = score / maxScore và raw_score của evidence đã có. */
    void onAssignmentGraded(UUID submissionId, BigDecimal score, BigDecimal maxScore);

    /**
     * Attempt chuyển sang GRADED. Chỉ gọi đúng 1 lần, ở lần chuyển trạng thái đó.
     * attemptNumber = 1 → ASSESSMENT, > 1 → REASSESSMENT.
     */
    void onAttemptGraded(UUID courseId, UUID studentId, UUID attemptId, UUID assessmentId,
                         int attemptNumber, int maxAttempts,
                         OffsetDateTime occurredAt, List<ObjectiveGroupScore> groups);
}
