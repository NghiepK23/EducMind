package com.educmind.auth.service;

import com.educmind.auth.entity.PasswordResetToken;
import com.educmind.auth.repository.PasswordResetTokenRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Sinh / xác thực mã đặt lại mật khẩu (Spec v2: AC-G02.2, AC-G02.3).
 * Server chỉ lưu SHA-256 hash, không lưu token thô.
 */
@Service
public class PasswordResetTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository repository;
    private final long ttlMinutes;

    public PasswordResetTokenService(
            PasswordResetTokenRepository repository,
            @Value("${educmind.password-reset.ttl-minutes:15}") long ttlMinutes
    ) {
        this.repository = repository;
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Tạo token mới cho user; mọi token cũ chưa dùng bị vô hiệu trước đó
     * (đồng thời thoả unique index uq_active_reset_token: 1 token active / user).
     */
    @Transactional
    public String issue(UUID userId) {
        repository.invalidateActiveTokens(userId, OffsetDateTime.now());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(new PasswordResetToken(
                userId,
                sha256(rawToken),
                OffsetDateTime.now().plusMinutes(ttlMinutes)
        ));
        return rawToken;
    }

    @Transactional
    public PasswordResetToken findUsableOrThrow(String rawToken) {
        PasswordResetToken token = repository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new BusinessException(
                        "RESET_TOKEN_INVALID",
                        "Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn",
                        HttpStatus.BAD_REQUEST));

        if (!token.isUsable()) {
            throw new BusinessException(
                    "RESET_TOKEN_INVALID",
                    "Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn",
                    HttpStatus.BAD_REQUEST);
        }
        return token;
    }

    public void markUsed(PasswordResetToken token) {
        token.setUsedAt(OffsetDateTime.now());
        repository.save(token);
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
