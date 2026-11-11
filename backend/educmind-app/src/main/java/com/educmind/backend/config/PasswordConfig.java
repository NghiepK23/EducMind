package com.educmind.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Tách khỏi SecurityConfig để tránh phụ thuộc vòng khi tạo bean. */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Hệ thống chỉ xác thực bằng JWT (không dùng form login / username-password của Spring
     * Security). Bean này tồn tại chỉ để Spring Boot KHÔNG tự tạo tài khoản in-memory
     * "user" kèm mật khẩu sinh ngẫu nhiên in ra log khi khởi động.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("EducMind xác thực bằng JWT, không có mật khẩu nội bộ");
        };
    }
}
