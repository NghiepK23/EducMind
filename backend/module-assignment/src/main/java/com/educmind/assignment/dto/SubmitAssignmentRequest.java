package com.educmind.assignment.dto;

import java.util.UUID;

public class SubmitAssignmentRequest {

    private String textAnswer;
    private UUID resourceId;

    public String getTextAnswer() {
        return textAnswer;
    }

    public void setTextAnswer(String textAnswer) {
        this.textAnswer = textAnswer;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }
}
