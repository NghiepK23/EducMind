package com.educmind.course.dto;

import jakarta.validation.constraints.Size;

/**
 * Request cho PATCH /api/v1/subjects/{id} — chỉ SCHOOL_ADMIN được gọi.
 *
 * Kiểu PATCH: mọi field đều nullable, field nào null trong request thì
 * SubjectService giữ nguyên giá trị cũ (không ghi đè) — cùng quy ước với
 * UpdateCourseRequest.subjectId (null nghĩa là không đổi).
 */
public class UpdateSubjectRequest {

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
