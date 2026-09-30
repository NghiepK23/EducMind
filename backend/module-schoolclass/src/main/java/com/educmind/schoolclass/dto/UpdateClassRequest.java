package com.educmind.schoolclass.dto;

/**
 * PATCH /api/v1/classes/{id} — Module01_Auth_SchoolClass_FINAL.md, mục B.6.
 * Mọi field đều nullable/optional: null = giữ nguyên giá trị cũ, không phải "xóa".
 * Không cho phép đổi ownerTeacherId hay schoolId qua endpoint này (đổi chủ nhiệm lớp
 * là 1 nghiệp vụ nhạy cảm hơn — nếu cần, nên có endpoint riêng, tránh lẫn vào PATCH chung).
 */
public class UpdateClassRequest {
    private String name;
    private String gradeLevel;
    private String academicYear;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
}
