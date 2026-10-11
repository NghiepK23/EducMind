package com.educmind.reports.dto;

import com.educmind.analytics.dto.MasteryOverviewResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ClassProgressReport(
        UUID courseId,
        UUID classId,
        OffsetDateTime generatedAt,
        MasteryOverviewResponse objectiveMasterySummary,
        List<AssignmentCompletionItem> assignmentCompletion,
        long openKnowledgeGapsCount) {
}
