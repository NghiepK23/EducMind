package com.educmind.course.dto;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Mọi field nullable — chỉ field nào client gửi (khác null) mới được LessonService
 * cập nhật, theo đúng pattern đã dùng ở UpdateCourseRequest.
 */
public class UpdateLessonRequest {

    private String title;
    private String description;

    @Valid
    private List<LearningObjectiveInput> learningObjectives;

    private Integer orderIndex;

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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
