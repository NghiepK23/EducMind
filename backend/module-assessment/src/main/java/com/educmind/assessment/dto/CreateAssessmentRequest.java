package com.educmind.assessment.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public class CreateAssessmentRequest {

    @NotBlank(message = "title không được để trống")
    private String title;

    private String description;

    private Integer durationMinutes;

    private Integer maxAttempts;

    private Object blueprintConfig;

    private OffsetDateTime opensAt;

    private OffsetDateTime closesAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Object getBlueprintConfig() {
        return blueprintConfig;
    }

    public void setBlueprintConfig(Object blueprintConfig) {
        this.blueprintConfig = blueprintConfig;
    }

    public OffsetDateTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(OffsetDateTime opensAt) {
        this.opensAt = opensAt;
    }

    public OffsetDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(OffsetDateTime closesAt) {
        this.closesAt = closesAt;
    }
}
