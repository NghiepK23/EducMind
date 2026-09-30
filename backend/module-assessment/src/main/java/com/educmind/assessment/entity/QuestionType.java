package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `questions.question_type` — V4__module_assignment_assessment.sql,
 * CHECK (question_type IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','TRUE_FALSE','SHORT_ANSWER','ESSAY')).
 * Module04_Assignment_Assessment_FINAL.md, mục E.4.
 */
public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER,
    ESSAY
}
