package com.educmind.course.dto;

import jakarta.validation.constraints.Size;

public class UpdateCourseRequest {

    @Size(min = 1, max = 200, message = "Tên khóa học phải từ 1 đến 200 ký tự")
    private String title;

    private String description;

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
}
