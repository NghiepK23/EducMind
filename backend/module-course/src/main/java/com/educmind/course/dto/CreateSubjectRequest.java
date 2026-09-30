package com.educmind.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request cho POST /api/v1/subjects — chỉ SCHOOL_ADMIN được gọi.
 *
 * Không có field schoolId: schoolId luôn được suy ra từ actor gọi API
 * (qua UserLookupPort ở SubjectService), giống nguyên tắc CreateCourseRequest
 * không có ownerTeacherId — tránh cho phép actor tự khai schoolId tùy ý.
 */
public class CreateSubjectRequest {

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 100, message = "Tên môn học tối đa 100 ký tự")
    private String name;

    @Size(max = 20, message = "Mã môn học tối đa 20 ký tự")
    private String code;

    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
