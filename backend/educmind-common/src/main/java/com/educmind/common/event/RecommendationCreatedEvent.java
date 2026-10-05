package com.educmind.common.event;

import java.util.UUID;

/** Phát khi recommendation mới được tạo (mọi type). */
public record RecommendationCreatedEvent(UUID recommendationId, UUID studentId, UUID courseId,
                                         UUID lessonId, UUID learningObjectiveId, String type) {
}
