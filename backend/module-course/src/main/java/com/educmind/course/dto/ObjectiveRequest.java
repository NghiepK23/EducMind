package com.educmind.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-T02 / AC-T02.2: mã mục tiêu duy nhất trong phạm vi bài học. */
public class ObjectiveRequest {

    @NotBlank(message = "Mã mục tiêu không được để trống")
    @Size(max = 20, message = "Mã mục tiêu tối đa 20 ký tự")
    private String code;

    @NotBlank(message = "Mô tả mục tiêu không được để trống")
    private String description;

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
