package com.educmind.course.dto;

import com.educmind.course.entity.BloomLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO input cho 1 mục tiêu học tập trong request tạo/sửa Lesson — KHÁC với entity-POJO
 * LearningObjective (không có field id, vì id do CourseService tự sinh UUID.randomUUID()
 * khi map sang LearningObjective, client không được tự gửi id lên).
 */
public class LearningObjectiveInput {

    @NotBlank(message = "text không được để trống")
    private String text;

    @NotNull(message = "bloomLevel không được để trống")
    private BloomLevel bloomLevel;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BloomLevel getBloomLevel() {
        return bloomLevel;
    }

    public void setBloomLevel(BloomLevel bloomLevel) {
        this.bloomLevel = bloomLevel;
    }
}
