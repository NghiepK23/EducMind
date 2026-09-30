package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `questions.cognitive_level` (NULLABLE) — V4__module_assignment_assessment.sql,
 * CHECK (cognitive_level IN ('REMEMBER','UNDERSTAND','APPLY','ANALYZE','EVALUATE','CREATE')).
 * Module04_Assignment_Assessment_FINAL.md, mục E.4 — [DECIDED] theo Bloom's Taxonomy.
 */
public enum CognitiveLevel {
    REMEMBER,
    UNDERSTAND,
    APPLY,
    ANALYZE,
    EVALUATE,
    CREATE
}
