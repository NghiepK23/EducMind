package com.educmind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request cho POST /api/v1/auth/change-password-forced. */
public class ChangePasswordForcedRequest {

    @NotBlank(message = "currentPassword khong duoc de trong")
    private String currentPassword;

    @NotBlank(message = "newPassword khong duoc de trong")
    @Size(min = 8, message = "mat khau moi phai co it nhat 8 ky tu")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
