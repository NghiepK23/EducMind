package com.educmind.course.dto;

import jakarta.validation.constraints.Size;

public class UpdateTopicRequest {

    @Size(min = 1, max = 200, message = "Tiêu đề chủ đề phải từ 1 đến 200 ký tự")
    private String title;

    private String description;

    private Integer displayOrder;

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
