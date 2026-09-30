package com.educmind.auth.repository;

import com.educmind.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(UUID userId);

    /**
     * [DECIDED — Module 1, mục A.5.5] Đổi mật khẩu thành công → revoke TOÀN BỘ refresh token
     * của user đó. Dùng bulk update (@Modifying) thay vì load từng entity rồi save từng cái
     * cho nhanh khi 1 user có nhiều refresh token (nhiều thiết bị đăng nhập cùng lúc).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now "
            + "WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
