package com.reactivecommerce.notification.application.usecase;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.port.in.StreamNotificationsUseCase;
import com.reactivecommerce.notification.domain.port.out.NotificationRepository;
import com.reactivecommerce.notification.domain.port.out.SsePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

/**
 * Combina notificaciones offline (persistidas no leídas) con el stream en vivo.
 *
 * Flujo al conectar:
 *  1. concatWith garantiza que primero llegan las no leídas almacenadas,
 *     luego el stream hot de nuevas notificaciones en tiempo real.
 *
 * Esto implementa el requisito HU-NOT-01 criterio 5:
 * "Notificaciones no entregadas (usuario offline) se entregan al reconectar".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamNotificationsUseCaseImpl implements StreamNotificationsUseCase {

    private final NotificationRepository notificationRepository;
    private final SsePublisherPort ssePublisherPort;

    @Override
    public Flux<Notification> stream(UUID userId) {
        Flux<Notification> pending = notificationRepository//mensajes sin leer antiguos
            .findUnreadByUserId(userId, 50)
            .doOnComplete(() -> log.debug("Pending notifications flushed for userId={}", userId));

        Flux<Notification> live = ssePublisherPort.streamForUser(userId);//conexión que escucha los nuevos mensajes

        // concatWith: pending primero (en orden), luego live (hot, indefinido)
        return pending.concatWith(live)
            .doOnSubscribe(s -> log.info("SSE stream opened for userId={}", userId))
            .doOnCancel(() -> log.info("SSE stream closed for userId={}", userId));
            //El usuario recibe sus mensajes antiguos primero y, sin que la conexión se cierre, empieza a recibir los nuevos en tiempo real
    }
}
