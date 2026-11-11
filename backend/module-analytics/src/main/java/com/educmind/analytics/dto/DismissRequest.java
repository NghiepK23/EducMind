package com.educmind.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AC-T08.4: giáo viên bỏ qua nội dung cần cải thiện và PHẢI nhập lý do. */
public class DismissRequest {

    @NotBlank(message = "Lý do bỏ qua không được để trống")
    @Size(max = 500, message = "Lý do bỏ qua tối đa 500 ký tự")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
