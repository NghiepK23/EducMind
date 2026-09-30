package com.educmind.schoolclass.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request cho POST /api/v1/classes — Module01_Auth_SchoolClass_FINAL.md, mục B.6.
 *
 * ownerTeacherId: tùy chọn ở tầng DTO/validation (@NotBlank không áp dụng cho UUID nên
 * không set ở đây), nhưng BẮT BUỘC về mặt nghiệp vụ khi actor gọi API là SCHOOL_ADMIN —
 * validate điều kiện này ở tầng Service (ClassService), không phải ở DTO, vì phụ thuộc
 * vào role của actor (thông tin chỉ có được sau khi đã xác thực, DTO không biết actor là ai).
 * Khi actor là TEACHER, field này bị BỎ QUA nếu có gửi lên — owner luôn là chính actor đó.
 */
public class CreateClassRequest {

    @NotBlank(message = "Tên lớp không được để trống")
    private String name;

    private String gradeLevel;

    private String academicYear;

    /** Chỉ dùng khi actor là SCHOOL_ADMIN — xem giải thích ở đầu class. */
    private UUID ownerTeacherId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public UUID getOwnerTeacherId() {
        return ownerTeacherId;
    }

    public void setOwnerTeacherId(UUID ownerTeacherId) {
        this.ownerTeacherId = ownerTeacherId;
    }
}
