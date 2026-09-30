package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `questions.source` — V4__module_assignment_assessment.sql,
 * CHECK (source IN ('MANUAL','AI_GENERATED')).
 * Module04_Assignment_Assessment_FINAL.md, mục E.4/E.5 — [ĐÃ MERGE, PATCH_4Decisions PATCH 1]
 * AI_GENERATED chỉ được INSERT lúc giáo viên APPROVE ở Module 7 (chưa code) — hiện tại
 * (Module 4, chưa có Module 7) mọi câu hỏi Nghiệp tự tạo đều dùng MANUAL.
 */
public enum QuestionSource {
    MANUAL,
    AI_GENERATED
}
