package com.educmind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request cho POST /api/v1/auth/password-reset/confirm. */
public class PasswordResetConfirmRequest {

    @NotBlank(message = "token khong duoc de trong")
    private String token;

    @NotBlank(message = "newPassword khong duoc de trong")
    @Size(min = 8, message = "mat khau moi phai co it nhat 8 ky tu")
    private String newPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
