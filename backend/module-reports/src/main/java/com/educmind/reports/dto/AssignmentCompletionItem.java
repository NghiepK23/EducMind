package com.educmind.reports.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentCompletionItem(
        UUID assignmentId,
        String title,
        OffsetDateTime dueDate,
        int studentCount,
        int submittedCount,
        double completionRate) {
}
