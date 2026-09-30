package com.educmind.course.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request cho POST /api/v1/courses/{id}/members/teachers — thêm 1 giáo viên
 * đồng giảng dạy vào course. Chỉ owner gốc mới gọi được endpoint này
 * (assertIsOwner) — theo checkpoint: "CHỈ owner course mới có quyền thêm/xóa
 * giáo viên khác vào course_members của CHÍNH course đó".
 */
public class AddTeacherToCourseRequest {

    @NotNull(message = "teacherId không được để trống")
    private UUID teacherId;

    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }
}
