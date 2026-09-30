package com.educmind.course.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request cho POST /api/v1/courses/{id}/members/students — thêm hàng loạt học
 * sinh từ 1 lớp hành chính vào course, theo model MỚI (xem checkpoint
 * "THAY ĐỔI KIẾN TRÚC LỚN"): course tự có course_members riêng, học sinh được
 * chọn ra từ 1 lớp hành chính (classes) cụ thể — không dùng course_class nữa.
 *
 * Chỉ owner gốc mới gọi được endpoint này (assertIsOwner, đã chốt với Nghiệp —
 * khác assertCanManage dùng cho các thao tác course thông thường khác).
 */
public class AddStudentsToCourseRequest {

    @NotNull(message = "sourceClassId không được để trống")
    private UUID sourceClassId;

    @NotEmpty(message = "studentIds không được rỗng")
    private List<UUID> studentIds;

    public UUID getSourceClassId() {
        return sourceClassId;
    }

    public void setSourceClassId(UUID sourceClassId) {
        this.sourceClassId = sourceClassId;
    }

    public List<UUID> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<UUID> studentIds) {
        this.studentIds = studentIds;
    }
}
