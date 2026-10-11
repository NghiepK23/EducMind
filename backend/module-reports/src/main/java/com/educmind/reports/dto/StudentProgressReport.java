package com.educmind.reports.dto;

import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.dto.RecommendationResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudentProgressReport(
        UUID studentId,
        UUID courseId,
        OffsetDateTime generatedAt,
        List<MasteryResponse> masteryByObjective,
        List<KnowledgeGapResponse> knowledgeGaps,
        List<RecommendationResponse> recommendations) {
}
