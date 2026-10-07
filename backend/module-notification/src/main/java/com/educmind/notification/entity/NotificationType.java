package com.educmind.notification.entity;

/** Các loại notification đã có nguồn sự kiện (Module 10 mục J.5.1). */
public final class NotificationType {

    public static final String ASSIGNMENT_PUBLISHED = "ASSIGNMENT_PUBLISHED";
    public static final String ASSIGNMENT_GRADED = "ASSIGNMENT_GRADED";
    public static final String ASSESSMENT_GRADED = "ASSESSMENT_GRADED";
    public static final String KNOWLEDGE_GAP_OPENED = "KNOWLEDGE_GAP_OPENED";
    public static final String RECOMMENDATION_NEW = "RECOMMENDATION_NEW";
    public static final String ASSIGNMENT_DUE_SOON = "ASSIGNMENT_DUE_SOON";
    public static final String SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";

    private NotificationType() {
    }
}
