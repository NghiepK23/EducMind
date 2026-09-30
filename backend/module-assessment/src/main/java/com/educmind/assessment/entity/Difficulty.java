package com.educmind.assessment.entity;

/**
 * Enum ứng với cột `questions.difficulty` — V4__module_assignment_assessment.sql,
 * CHECK (difficulty IN ('EASY','MEDIUM','HARD')).
 * Module04_Assignment_Assessment_FINAL.md, mục E.4.
 * [TBD] Thang đo cụ thể (dựa trên tiêu chí gì) — Master_TBD_Tracker mục 42/43,
 * không chặn việc dùng enum này, chỉ chưa có tiêu chí phân loại chính thức.
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
}
