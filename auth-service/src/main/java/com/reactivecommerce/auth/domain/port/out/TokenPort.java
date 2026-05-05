package com.reactivecommerce.auth.domain.port.out;

import com.reactivecommerce.auth.domain.model.TokenPair;
import com.reactivecommerce.auth.domain.model.User;

import java.util.UUID;

/**
 * Puerto de salida para generación y validación de tokens de autenticación.
 *
 * La capa de aplicación (use cases) solo conoce este puerto.
 * La implementación concreta (JwtService con JJWT) vive en infraestructura
 * y se inyecta por inversión de dependencias.
 *
 * Esto permite cambiar el mecanismo de tokens (JWT → Paseto, opaque tokens,
 * etc.) sin tocar ningún use case.
 */
public interface TokenPort {

    /** Genera un par access/refresh token para el usuario dado. */
    TokenPair generateTokenPair(User user);

    /** Extrae el userId del subject del token. */
    UUID extractUserId(String token);

    /** Extrae el tipo del token ("access" o "refresh"). */
    String extractType(String token);

    /** Devuelve true si el token tiene firma válida y no ha expirado. */
    boolean isValid(String token);
}
