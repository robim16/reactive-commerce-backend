package com.reactivecommerce.notification.domain.port.in;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Caso de uso de escritura: marcar notificaciones como leídas.
 * Invocado desde el NotificationHandler en las rutas PUT.
 */
public interface MarkNotificationsReadUseCase {
    Mono<Void> markOne(String notificationId);
    Mono<Void> markAll(UUID userId);
}
