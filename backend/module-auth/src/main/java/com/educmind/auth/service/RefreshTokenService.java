package com.educmind.auth.service;

import com.educmind.auth.entity.RefreshToken;
import com.educmind.auth.repository.RefreshTokenRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Sinh, hash, và validate refresh token — theo spec Module 1, mục A.4 (bảng refresh_tokens).
 * Token thô (plaintext) chỉ tồn tại tạm thời trong bộ nhớ để trả về client 1 lần duy nhất
 * lúc login — server chỉ lưu SHA-256 hash, không lưu lại token gốc.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${educmind.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    /** Sinh 1 refresh token mới cho user, lưu hash vào DB, trả về token THÔ (chỉ dùng 1 lần). */
    public String issueRefreshToken(UUID userId) {
        String rawToken = generateRawToken();
        String hash = sha256(rawToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(refreshTokenTtlDays);

        RefreshToken entity = new RefreshToken(userId, hash, expiresAt);
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    /**
     * Validate 1 refresh token tho: hash len, tim trong DB, kiem tra con han/chua bi revoke
     * (RefreshToken.isValid()). Dung cho luong POST /auth/refresh va POST /auth/logout.
     * Nem BusinessException(TOKEN_EXPIRED, 401) neu khong tim thay hoac khong hop le.
     */
    public RefreshToken findValidTokenOrThrow(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(
                        "TOKEN_EXPIRED",
                        "Refresh token khong hop le hoac da het han",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!token.isValid()) {
            throw new BusinessException(
                    "TOKEN_EXPIRED",
                    "Refresh token khong hop le hoac da het han",
                    HttpStatus.UNAUTHORIZED
            );
        }

        return token;
    }

    /** Revoke 1 refresh token cu the (dung chung cho rotation o /refresh va cho /logout). */
    public void revokeToken(RefreshToken token) {
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
