package com.reactivecommerce.notification.domain.port.in;

import com.reactivecommerce.notification.domain.model.Notification;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Caso de uso de consulta: obtener notificaciones no leídas del usuario.
 * Invocado desde el NotificationHandler al listar y al abrir el stream SSE.
 */
public interface GetNotificationsUseCase {
    Flux<Notification> findUnread(UUID userId, int limit);
}
