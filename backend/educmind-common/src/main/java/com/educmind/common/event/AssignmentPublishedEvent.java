package com.educmind.common.event;

import java.util.UUID;

/** Phát khi assignment được publish. Người nhận = học sinh ACTIVE của course (Module 10 tự tra qua port). */
public record AssignmentPublishedEvent(UUID assignmentId, UUID courseId, String title) {
}
