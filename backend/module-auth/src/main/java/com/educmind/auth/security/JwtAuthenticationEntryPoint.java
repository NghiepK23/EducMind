package com.educmind.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Trả 401 đúng format ApiResponse khi thiếu/sai/hết hạn token. */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY = "{"
            + "\"success\":false,"
            + "\"data\":null,"
            + "\"error\":{"
            + "\"code\":\"TOKEN_EXPIRED\","
            + "\"message\":\"Token không hợp lệ hoặc đã hết hạn\""
            + "},"
            + "\"meta\":{}"
            + "}";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}
