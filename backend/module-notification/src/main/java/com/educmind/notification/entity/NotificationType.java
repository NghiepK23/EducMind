package com.educmind.notification.entity;

/** Đúng enum notification_type của Spec v2 (mục B). Thông báo do trigger DB sinh. */
public enum NotificationType {
    NEW_SUBMISSION,
    SUBMISSION_RESUBMITTED,
    NEW_ASSESSMENT_ATTEMPT_SUBMITTED,
    AI_GENERATION_COMPLETED,
    AI_GENERATION_FAILED,
    NEW_LESSON_PUBLISHED,
    NEW_ASSIGNMENT_PUBLISHED,
    NEW_ASSESSMENT_PUBLISHED,
    ASSIGNMENT_GRADED,
    ASSESSMENT_GRADED,
    NEW_RECOMMENDATION
}
