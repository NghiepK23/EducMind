package com.educmind.assignment.dto;

import com.educmind.assignment.entity.Assignment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(UUID id, UUID courseId, UUID topicId, UUID lessonId,
                                 String title, String description, OffsetDateTime dueDate,
                                 BigDecimal maxScore, boolean allowLate, OffsetDateTime lateCloseDate,
                                 String status, List<UUID> objectiveIds, long submissionCount,
                                 OffsetDateTime createdAt) {

    public static AssignmentResponse from(Assignment a, List<UUID> objectiveIds, long submissionCount) {
        return new AssignmentResponse(a.getId(), a.getCourseId(), a.getTopicId(), a.getLessonId(),
                a.getTitle(), a.getDescription(), a.getDueDate(), a.getMaxScore(), a.isAllowLate(),
                a.getLateCloseDate(), a.getStatus().name(), objectiveIds, submissionCount, a.getCreatedAt());
    }
}
