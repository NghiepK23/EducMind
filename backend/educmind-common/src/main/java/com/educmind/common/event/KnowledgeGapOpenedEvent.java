package com.educmind.common.event;

import java.util.UUID;

/** Phát khi knowledge gap mới được mở (không phát khi resolve). */
public record KnowledgeGapOpenedEvent(UUID gapId, UUID studentId, UUID courseId,
                                      UUID lessonId, UUID learningObjectiveId) {
}
