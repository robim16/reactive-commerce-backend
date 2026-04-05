package com.reactivecommerce.auth.infrastructure.config;

import com.reactivecommerce.auth.domain.model.TokenPair;
import com.reactivecommerce.auth.domain.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-ttl-ms:900000}") long accessTokenTtlMs,
        @Value("${jwt.refresh-token-ttl-ms:604800000}") long refreshTokenTtlMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMs = accessTokenTtlMs;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    public TokenPair generateTokenPair(User user) {
        String accessToken = buildToken(user, accessTokenTtlMs, "access");
        String refreshToken = buildToken(user, refreshTokenTtlMs, "refresh");
        return new TokenPair(accessToken, refreshToken);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private String buildToken(User user, long ttlMs, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.id().toString())
            .claim("role", user.role().name())
            .claim("email", user.email())
            .claim("type", type)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(ttlMs)))
            .signWith(secretKey)
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
            .parseSignedClaims(token).getPayload();
    }
}
