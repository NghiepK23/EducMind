package com.educmind.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request cho POST /api/v1/auth/password-reset/request. */
public class PasswordResetRequestRequest {

    @NotBlank(message = "email khong duoc de trong")
    @Email(message = "email khong dung dinh dang")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
