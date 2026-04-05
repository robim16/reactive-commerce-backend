package com.reactivecommerce.notification.domain.port.in;

import com.reactivecommerce.notification.domain.model.Notification;
import reactor.core.publisher.Flux;
import java.util.UUID;

/**
 * Caso de uso de streaming SSE: abre el Flux hot para un usuario.
 * Combina notificaciones pendientes (persistidas no leídas) con el stream en vivo.
 * Invocado desde el NotificationHandler en GET /stream.
 */
public interface StreamNotificationsUseCase {
    Flux<Notification> stream(UUID userId);
}
