package com.reactivecommerce.notification.infrastructure.entrypoint.web;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.port.in.GetNotificationsUseCase;
import com.reactivecommerce.notification.domain.port.in.MarkNotificationsReadUseCase;
import com.reactivecommerce.notification.domain.port.in.StreamNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Handler reactivo del Notification Service.
 * Delega toda la lógica a los use cases correspondientes.
 * No accede directamente a ningún puerto de salida.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler {

    private final StreamNotificationsUseCase streamNotificationsUseCase;
    private final GetNotificationsUseCase getNotificationsUseCase;
    private final MarkNotificationsReadUseCase markNotificationsReadUseCase;

    /**
     * GET /api/v1/notifications/stream
     * Abre un stream SSE para el usuario autenticado.
     * Reenvía primero las no leídas pendientes y luego emite en tiempo real.
     * Heartbeat cada 30s para mantener la conexión activa a traves del Gateway.
     */
    public Mono<ServerResponse> stream(ServerRequest request) {
        UUID userId = extractUserId(request);

        Flux<ServerSentEvent<Notification>> notifications = streamNotificationsUseCase
            .stream(userId)
            .map(n -> ServerSentEvent.<Notification>builder()
                .id(n.id())
                .event("notification")
                .data(n)
                .build());

        Flux<ServerSentEvent<Notification>> heartbeat = Flux
            .interval(Duration.ofSeconds(30))
            .map(tick -> ServerSentEvent.<Notification>builder()
                .event("heartbeat")
                .comment("keep-alive")
                .build());

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(Flux.merge(notifications, heartbeat), ServerSentEvent.class);
    }

    /**
     * GET /api/v1/notifications
     * Lista las notificaciones no leídas del usuario (máx 50 por defecto).
     */
    public Mono<ServerResponse> list(ServerRequest request) {
        UUID userId = extractUserId(request);
        int limit = request.queryParam("limit").map(Integer::parseInt).orElse(50);
        return ServerResponse.ok()
            .body(getNotificationsUseCase.findUnread(userId, limit), Notification.class);
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Marca todas las notificaciones del usuario como leídas.
     */
    public Mono<ServerResponse> markAllRead(ServerRequest request) {
        UUID userId = extractUserId(request);
        return markNotificationsReadUseCase.markAll(userId)
            .then(ServerResponse.noContent().build());
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     * Marca una notificación específica como leída.
     */
    public Mono<ServerResponse> markRead(ServerRequest request) {
        String id = request.pathVariable("id");
        return markNotificationsReadUseCase.markOne(id)
            .then(ServerResponse.noContent().build());
    }

    private UUID extractUserId(ServerRequest request) {
        return UUID.fromString(request.headers().firstHeader("X-User-Id"));
    }
}
