package com.educmind.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Sinh và verify JWT access token.
 * TTL access token: [TBD trong spec Module 1, mục A.5] — tạm chốt 15 phút cho môi trường dev,
 * cần xác nhận chính thức trước khi lên production (xem Phase10 Checklist, mục L.5.1).
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenTtlMillis;

    public JwtService(
            @Value("${educmind.jwt.secret}") String secret,
            @Value("${educmind.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenTtlMillis = accessTokenTtlMinutes * 60 * 1000;
    }

    public String generateAccessToken(UUID userId, String role, UUID schoolId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenTtlMillis);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey);

        if (schoolId != null) {
            builder.claim("school_id", schoolId.toString());
        }

        return builder.compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseAndValidate(token).getSubject());
    }

    public String extractRole(String token) {
        return parseAndValidate(token).get("role", String.class);
    }
}
