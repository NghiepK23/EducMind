package com.educmind.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class SaveAnswerRequest {

    @NotNull(message = "questionId không được để trống")
    private UUID questionId;

    private Object studentAnswer;

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public Object getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(Object studentAnswer) {
        this.studentAnswer = studentAnswer;
    }
}
