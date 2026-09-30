package com.educmind.course.dto;

/**
 * PATCH /api/v1/courses/{id} — Module03_Course_Lesson_Resource_FINAL.md, mục C.6.
 * Mọi field đều nullable/optional: null = giữ nguyên giá trị cũ, không phải "xóa".
 * Không có annotation validation ở đây (giống UpdateClassRequest) — nếu field có gửi lên
 * nhưng rỗng, validate "hasText" được làm thủ công ở CourseService, vì null và "" mang
 * ý nghĩa khác nhau (null = không đổi, "" = cố tình xóa nhưng không hợp lệ với 1 field
 * bắt buộc như name).
 * Không cho phép đổi schoolId hay createdBy qua endpoint này — đổi owner gốc là nghiệp vụ
 * riêng (đã note trong checkpoint: "Owner chỉ độc quyền... (b) đổi created_by").
 */
public class UpdateCourseRequest {
    private String name;
    private String description;
    private java.util.UUID subjectId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.util.UUID getSubjectId() { return subjectId; }
    public void setSubjectId(java.util.UUID subjectId) { this.subjectId = subjectId; }
}
