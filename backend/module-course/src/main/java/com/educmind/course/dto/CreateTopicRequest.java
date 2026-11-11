package com.educmind.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTopicRequest {

    @NotBlank(message = "Tiêu đề chủ đề không được để trống")
    @Size(max = 200, message = "Tiêu đề chủ đề tối đa 200 ký tự")
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
