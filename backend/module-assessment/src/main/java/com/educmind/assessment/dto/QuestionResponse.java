package com.educmind.assessment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(UUID id, UUID courseId, String content, String type, String difficulty,
                               String bloomLevel, String source, List<String> options,
                               List<Integer> correctOptionIndexes, String expectedAnswer,
                               List<UUID> objectiveIds, OffsetDateTime createdAt) {
}
