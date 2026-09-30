package com.educmind.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Request tạo Lesson mới — POST /api/v1/courses/{id}/lessons.
 * learningObjectives mặc định rỗng (giáo viên có thể tạo lesson trước, thêm objective sau
 * qua updateLesson); orderIndex mặc định 0 (giáo viên có thể sắp lại thứ tự sau).
 */
public class CreateLessonRequest {

    @NotBlank(message = "title không được để trống")
    private String title;

    private String description;

    @Valid
    private List<LearningObjectiveInput> learningObjectives = new ArrayList<>();

    private int orderIndex = 0;

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

    public List<LearningObjectiveInput> getLearningObjectives() {
        return learningObjectives;
    }

    public void setLearningObjectives(List<LearningObjectiveInput> learningObjectives) {
        this.learningObjectives = learningObjectives;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
