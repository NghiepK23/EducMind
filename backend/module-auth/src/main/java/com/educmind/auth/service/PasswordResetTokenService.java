package com.educmind.auth.service;

import com.educmind.auth.entity.PasswordResetToken;
import com.educmind.auth.repository.PasswordResetTokenRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Sinh, hash, va validate password reset token - Module 1, muc A.4 + A.5.4.
 * Cung mo hinh voi RefreshTokenService: token tho chi ton tai tam trong bo nho de
 * dua vao email 1 lan duy nhat, server chi luu SHA-256 hash.
 * TTL (15 phut) da duoc [DECIDED] va hardcode trong PasswordResetToken.TTL, khong
 * can tham so hoa nhu RefreshTokenService (RefreshToken TTL 7 ngay la [TBD], con
 * PasswordResetToken TTL 15 phut la [DECIDED] - xem javadoc entity).
 */
@Service
public class PasswordResetTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /** Sinh 1 reset token moi cho user, luu hash vao DB, tra ve token THO (chi dung 1 lan, de gui qua email). */
    public String issueResetToken(UUID userId) {
        String rawToken = generateRawToken();
        String hash = sha256(rawToken);

        PasswordResetToken entity = new PasswordResetToken(userId, hash);
        passwordResetTokenRepository.save(entity);

        return rawToken;
    }

    /**
     * Validate 1 reset token tho: hash len, tim trong DB, kiem tra con han/chua dung
     * (PasswordResetToken.isUsable()). Nem BusinessException(RESET_TOKEN_INVALID, 400)
     * neu khong tim thay hoac khong hop le - dung ma loi theo spec Module 1 muc A.8.
     */
    public PasswordResetToken findValidTokenOrThrow(String rawToken) {
        String hash = sha256(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(
                        "RESET_TOKEN_INVALID",
                        "Password reset token khong hop le hoac da het han",
                        HttpStatus.BAD_REQUEST
                ));

        if (!token.isUsable()) {
            throw new BusinessException(
                    "RESET_TOKEN_INVALID",
                    "Password reset token khong hop le hoac da het han",
                    HttpStatus.BAD_REQUEST
            );
        }

        return token;
    }

    /** Danh dau 1 token da dung (dung 1 lan duy nhat - [DECIDED] Module 1 muc A.5.4). */
    public void markUsed(PasswordResetToken token) {
        token.markUsed();
        passwordResetTokenRepository.save(token);
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
