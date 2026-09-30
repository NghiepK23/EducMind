package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `assessments.publish_status` — V4__module_assignment_assessment.sql,
 * CHECK (publish_status IN ('DRAFT','PUBLISHED')).
 * Module04_Assignment_Assessment_FINAL.md, mục E.4/E.5.
 * Đặt tên riêng (không tái dùng PublishStatus của module-course) — mỗi module tự định
 * nghĩa enum riêng dù trùng tên/giá trị, đúng nguyên tắc cô lập cross-module đã chốt.
 */
public enum AssessmentPublishStatus {
    DRAFT,
    PUBLISHED
}
