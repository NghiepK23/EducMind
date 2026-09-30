package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `questions.review_status` — V4__module_assignment_assessment.sql,
 * CHECK (review_status IN ('PENDING','APPROVED','REJECTED')), DEFAULT 'APPROVED'.
 * Module04_Assignment_Assessment_FINAL.md, mục E.5 — [ĐÃ MERGE, PATCH_4Decisions PATCH 1]
 * PENDING không bao giờ phát sinh thực tế cho source=AI_GENERATED (dòng chưa duyệt thì
 * chưa tồn tại); PENDING chỉ có ý nghĩa cho source=MANUAL nếu UI cho phép lưu nháp
 * (hiện tại Module 4 luôn tạo MANUAL với APPROVED ngay, PENDING chưa được dùng thực tế).
 */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED
}
