package com.educmind.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.envelope.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Đọc header Authorization: Bearer <token>, validate qua JwtService,
 * và gán danh tính (userId + role) vào SecurityContext cho request hiện tại.
 * Nếu không có token / token sai / hết hạn -> KHÔNG set Authentication,
 * để SecurityConfig tự quyết định chặn (401/403) dựa trên anyRequest().authenticated().
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_FORCED_PATH = "/api/v1/auth/change-password-forced";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            UUID userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // [Module 1, muc A.5 rule 7] Neu tai khoan dang o trang thai bat buoc doi mat khau,
            // chan moi endpoint khac ngoai duy nhat change-password-forced. Query DB moi request
            // (khong nhung co vao JWT claim) de luon phan anh dung trang thai thoi gian thuc -
            // [DECIDED, Nghiep xac nhan] danh doi 1 DB call/request de lay do chinh xac.
            if (!request.getRequestURI().equals(CHANGE_PASSWORD_FORCED_PATH)) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.isMustChangePassword()) {
                    writeMustChangePasswordError(response);
                    return;
                }
            }
        } catch (Exception e) {
            // Token không hợp lệ / hết hạn -> để trống context, không tự trả lỗi ở đây
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void writeMustChangePasswordError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(
                "MUST_CHANGE_PASSWORD",
                "Tai khoan phai doi mat khau truoc khi thuc hien thao tac khac"
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
