package com.educmind.course.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UpdateLessonRequest {

    @Size(min = 1, max = 200, message = "Tiêu đề bài học phải từ 1 đến 200 ký tự")
    private String title;

    private String description;

    private UUID topicId;

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

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }
}
