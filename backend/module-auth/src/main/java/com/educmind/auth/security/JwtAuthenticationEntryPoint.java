package com.educmind.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý mọi trường hợp request KHÔNG có/token sai/token hết hạn khi gọi vào
 * endpoint yêu cầu authenticated(). Trả đúng 401 (không phải 403 mặc định
 * của Spring Security) + đúng format ApiResponse chung của toàn hệ thống.
 *
 * Lưu ý: viết JSON thủ công bằng String (không dùng Jackson ObjectMapper)
 * vì module-auth hiện chưa có dependency Jackson riêng. Nội dung body ở đây
 * là cố định (không chèn dữ liệu người dùng), nên không có rủi ro JSON injection.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY = "{"
            + "\"success\":false,"
            + "\"data\":null,"
            + "\"error\":{"
            + "\"code\":\"TOKEN_EXPIRED\","
            + "\"message\":\"Token khong hop le hoac da het han\""
            + "},"
            + "\"meta\":{}"
            + "}";

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}
