package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.port.out.EmailVerificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Adaptador Redis para tokens de verificación de email.
 *
 * Estructura de clave:  email_verification:{token}  →  {userId}
 * TTL:                  24 horas (configurable vía EMAIL_VERIFICATION_TTL_HOURS)
 *
 * El token es un UUID aleatorio generado aquí; en producción se empaqueta
 * dentro del link de verificación que el Notification Service envía vía SES.
 *
 * Implementa EmailVerificationPort (puerto de salida del dominio), por lo que
 * el dominio nunca conoce que el almacenamiento es Redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationAdapter implements EmailVerificationPort {

    private static final String PREFIX = "email_verification:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Crea y almacena un token de verificación para el userId dado.
     * Devuelve el token generado para que el caller pueda incluirlo en el email.
     */
    @Override
    public Mono<String> createVerificationToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        String key   = PREFIX + token;

        return redisTemplate.opsForValue()
                .set(key, userId.toString(), TOKEN_TTL)
                .thenReturn(token)
                .doOnSuccess(t -> log.debug(
                        "Verification token created for userId={} ttl={}h", userId, TOKEN_TTL.toHours()));
    }

    /**
     * Valida el token y devuelve el userId asociado.
     * Devuelve Mono.empty() si el token no existe o ya expiró (Redis lo elimina solo).
     */
    @Override
    public Mono<UUID> validateToken(String token) {
        return redisTemplate.opsForValue()
                .get(PREFIX + token)
                .map(UUID::fromString)
                .doOnSuccess(userId -> {
                    if (userId != null) {
                        log.debug("Token validated for userId={}", userId);
                    }
                });
    }

    /**
     * Elimina el token después de su uso para garantizar uso único.
     */
    @Override
    public Mono<Void> deleteToken(String token) {
        return redisTemplate.delete(PREFIX + token)
                .then()
                .doOnSuccess(v -> log.debug("Verification token deleted: {}", token));
    }
}
