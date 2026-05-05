package com.reactivecommerce.auth.infrastructure.entrypoint.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * DTO de error estándar para todas las respuestas de error del auth-service.
 *
 * Estructura:
 * {
 *   "status": 400,
 *   "message": "El email ya está registrado",
 *   "timestamp": "2026-04-26T21:32:23.992Z"
 * }
 *
 * El campo status es redundante con el HTTP status code pero útil para
 * clientes que no siempre inspeccionan el código de respuesta HTTP.
 */
public record ErrorResponse(
    int    status,
    String message,

    @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                timezone = "UTC")
    Instant timestamp
) {
    /** Usado por GlobalExceptionHandler — incluye el status HTTP. */
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now());
    }

    /**
     * Compatibilidad con los handlers que aún usan ErrorResponse.of(message).
     * Los handlers pueden eliminarse progresivamente al confiar en el handler global.
     */
    public static ErrorResponse of(String message) {
        return new ErrorResponse(0, message, Instant.now());
    }
}
