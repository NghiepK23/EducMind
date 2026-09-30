package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `assessment_attempts.status` — V4__module_assignment_assessment.sql,
 * CHECK (status IN ('IN_PROGRESS','SUBMITTED','GRADED')).
 * Module04_Assignment_Assessment_FINAL.md, mục E.4/E.5.
 */
public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    GRADED
}
