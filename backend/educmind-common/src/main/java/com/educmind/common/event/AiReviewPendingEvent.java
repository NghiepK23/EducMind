package com.educmind.common.event;

import java.util.UUID;

/** Phát khi AI sinh xong nội dung và đang chờ giáo viên duyệt (Module 10 mục J.5.1, AI_REVIEW_PENDING). */
public record AiReviewPendingEvent(UUID requestId, UUID teacherId, UUID courseId, UUID lessonId, String requestType) {
}
