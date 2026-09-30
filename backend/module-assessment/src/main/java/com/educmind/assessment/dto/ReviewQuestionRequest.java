package com.educmind.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public class ReviewQuestionRequest {

    @NotBlank(message = "action không được để trống")
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
