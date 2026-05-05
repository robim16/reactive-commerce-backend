package com.reactivecommerce.auth.infrastructure.entrypoint.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Manejador global de excepciones para Spring WebFlux.
 *
 * ¿POR QUÉ WebExceptionHandler y no @ControllerAdvice?
 * ─────────────────────────────────────────────────────
 * @ControllerAdvice solo funciona con @RestController. Las RouterFunction
 * con HandlerFunction no pasan por el DispatcherServlet que invoca los
 * @ControllerAdvice — las excepciones no capturadas en el handler simplemente
 * se propagan hasta el DefaultErrorWebExceptionHandler de Spring Boot,
 * que devuelve un JSON genérico con status 500 sin el cuerpo esperado.
 *
 * WebExceptionHandler se registra directamente en la cadena de filtros
 * del WebFlux pipeline (antes del router), capturando CUALQUIER excepción
 * no manejada independientemente de cómo esté definida la ruta.
 *
 * @Order(-2): Spring Boot registra su DefaultErrorWebExceptionHandler
 * con @Order(-1). Este handler debe ejecutarse ANTES con @Order(-2).
 *
 * ESTRATEGIA DE MAPEO:
 * ─────────────────────
 * Cada tipo de excepción se mapea a un HTTP status semánticamente correcto.
 * Las excepciones de negocio (IllegalArgumentException, IllegalStateException)
 * también se capturan aquí como fallback, aunque los handlers individuales
 * pueden manejarlas primero con onErrorResume si necesitan lógica específica.
 *
 * SIMPLIFICACIÓN DE HANDLERS:
 * ────────────────────────────
 * Con este handler global, AuthHandler (y cualquier otro handler del servicio)
 * puede eliminar todos sus onErrorResume. Si una excepción escapa del handler,
 * este componente la intercepta y construye la respuesta correcta.
 */
@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;

        // ── Excepciones de negocio ─────────────────────────────────────────
        if (ex instanceof IllegalArgumentException) {
            status  = HttpStatus.BAD_REQUEST;
            message = ex.getMessage();

        } else if (ex instanceof IllegalStateException) {
            // Conflictos de negocio (ej: email ya registrado)
            status  = HttpStatus.CONFLICT;
            message = ex.getMessage();

        // ── Excepciones de Spring Data / R2DBC ────────────────────────────
        } else if (ex instanceof DataIntegrityViolationException) {
            // Violación de constraint único (email duplicado a nivel de BD)
            // como defensa adicional si el use case no detectó el duplicado
            status  = HttpStatus.CONFLICT;
            message = "El recurso ya existe o viola una restricción de integridad";
            log.warn("DataIntegrityViolation: {}", ex.getMessage());

        } else if (ex instanceof TransientDataAccessResourceException) {
            // "Row with Id [...] does not exist" — INSERT vs UPDATE incorrecto
            // Esto no debería ocurrir con el fix de Persistable<UUID>,
            // pero se captura como red de seguridad
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error de persistencia: operación de base de datos inválida";
            log.error("TransientDataAccess error (possible INSERT/UPDATE mismatch): {}",
                ex.getMessage(), ex);

        // ── Excepciones de Spring WebFlux ─────────────────────────────────
        } else if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();

        // ── NullPointerException (body vacío, campos null) ─────────────────
        } else if (ex instanceof NullPointerException) {
            status  = HttpStatus.BAD_REQUEST;
            message = "El cuerpo de la petición contiene campos nulos o ausentes";
            log.warn("NullPointerException in request processing: {}", ex.getMessage());

        // ── Cualquier otra excepción no anticipada ─────────────────────────
        } else {
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del servidor";
            log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(),
                ex.getMessage(), ex);
        }

        return writeErrorResponse(exchange, status, message);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange,
                                          HttpStatus status,
                                          String message) {
        try {
            ErrorResponse body  = ErrorResponse.of(status.value(), message);
            byte[]        bytes = objectMapper.writeValueAsBytes(body);

            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(bytes);

            return exchange.getResponse().writeWith(Mono.just(buffer));

        } catch (Exception e) {
            log.error("Failed to serialize error response", e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }
}
