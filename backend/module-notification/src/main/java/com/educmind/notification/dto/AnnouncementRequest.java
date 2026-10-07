package com.educmind.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Nội dung broadcast do SCHOOL_ADMIN hoặc SUPER_ADMIN tạo. */
public class AnnouncementRequest {

    @NotBlank(message = "title không được để trống")
    @Size(max = 200, message = "title không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "body không được để trống")
    @Size(max = 1000, message = "body không được vượt quá 1000 ký tự")
    private String body;

    /** null = hành vi mặc định; "SCHOOL" hoặc "CLASS" (Module 10, J.6.3). */
    private String scope;

    /** Bắt buộc khi scope=CLASS. */
    private UUID classId;

    /** Chỉ cần khi SUPER_ADMIN dùng scope=SCHOOL; SCHOOL_ADMIN bỏ qua (dùng trường của mình). */
    private UUID schoolId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public UUID getClassId() {
        return classId;
    }

    public void setClassId(UUID classId) {
        this.classId = classId;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }
}
