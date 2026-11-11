package com.educmind.assessment.entity;

/** Spec v2: question_type. */
public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER,
    ESSAY;

    public boolean isChoice() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE || this == TRUE_FALSE;
    }
}
