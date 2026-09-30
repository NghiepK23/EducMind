package com.educmind.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request cho POST /api/v1/courses — Module03_Course_Lesson_Resource_FINAL.md, mục C.6.
 *
 * Không có field kiểu ownerTeacherId như CreateClassRequest: endpoint này CHỈ dành cho
 * TEACHER (không có nhánh SCHOOL_ADMIN tạo hộ), nên createdBy luôn là chính actor gọi API —
 * CourseService không cần resolve owner phức tạp như ClassService.createClass().
 */
public class CreateCourseRequest {

    @NotBlank(message = "Tên khóa học không được để trống")
    private String name;

    private String description;

    @NotNull(message = "subjectId không được để trống")
    private java.util.UUID subjectId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.util.UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(java.util.UUID subjectId) {
        this.subjectId = subjectId;
    }
}
