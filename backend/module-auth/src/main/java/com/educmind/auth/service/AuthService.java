package com.educmind.auth.service;

import com.educmind.auth.dto.ChangePasswordRequest;
import com.educmind.auth.dto.LoginRequest;
import com.educmind.auth.dto.LoginResponse;
import com.educmind.auth.dto.PasswordResetConfirmRequest;
import com.educmind.auth.dto.PasswordResetRequestRequest;
import com.educmind.auth.dto.UserResponse;
import com.educmind.auth.entity.PasswordResetToken;
import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.auth.security.JwtService;
import com.educmind.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Luồng xác thực theo Spec v2: US-G01 (đăng nhập/đăng xuất), US-G02 (quên mật khẩu),
 * US-G04 (đổi mật khẩu).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenService resetTokenService;
    private final EmailService emailService;
    private final int passwordMinLength;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PasswordResetTokenService resetTokenService,
                       EmailService emailService,
                       @Value("${educmind.security.password-min-length:8}") int passwordMinLength) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resetTokenService = resetTokenService;
        this.emailService = emailService;
        this.passwordMinLength = passwordMinLength;
    }

    /** US-G01. Sai email hoặc sai mật khẩu trả cùng một lỗi để không lộ email tồn tại. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail().trim())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        String token = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        return LoginResponse.of(token, jwtService.getTtlSeconds(), UserResponse.from(user));
    }

    /** US-G04. Đổi mật khẩu khi đang đăng nhập. */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("CURRENT_PASSWORD_INVALID",
                    "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST);
        }
        requireStrongEnough(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /** US-G02 — AC-G02.1: email không tồn tại vẫn trả thành công (chống dò email). */
    @Transactional
    public void requestPasswordReset(PasswordResetRequestRequest request) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail().trim())
                .ifPresent(user -> {
                    String rawToken = resetTokenService.issue(user.getId());
                    emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
                });
    }

    /** US-G02 — AC-G02.2/3: token dùng một lần, mã cũ bị vô hiệu khi tạo mã mới. */
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = resetTokenService.findUsableOrThrow(request.getToken());
        User user = userRepository.findByIdAndDeletedAtIsNull(token.getUserId())
                .orElseThrow(() -> new BusinessException("RESET_TOKEN_INVALID",
                        "Mã đặt lại mật khẩu không hợp lệ", HttpStatus.BAD_REQUEST));

        requireStrongEnough(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetTokenService.markUsed(token);
        log.info("Đã đặt lại mật khẩu cho user {}", user.getId());
    }

    /**
     * US-G01.3 — access token không trạng thái phía server, nên đăng xuất là việc client
     * bỏ token. Endpoint giữ lại để client gọi thống nhất và để log/audit về sau.
     */
    public void logout(UUID userId) {
        log.info("User {} đăng xuất", userId);
    }

    private void requireStrongEnough(String password) {
        if (password == null || password.length() < passwordMinLength) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Mật khẩu phải có ít nhất " + passwordMinLength + " ký tự",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS",
                "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
    }
}
