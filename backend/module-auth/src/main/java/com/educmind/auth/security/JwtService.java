package com.educmind.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Sinh và xác thực access token (Spec v2 không có refresh token — xem
 * docs/spec-v2/03-GAP-ANALYSIS.md mục 6).
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long ttlSeconds;

    public JwtService(
            @Value("${educmind.jwt.secret}") String secret,
            @Value("${educmind.jwt.access-token-ttl-minutes:60}") long ttlMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlMinutes * 60;
    }

    public String generateAccessToken(UUID userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlSeconds * 1000);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
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

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
