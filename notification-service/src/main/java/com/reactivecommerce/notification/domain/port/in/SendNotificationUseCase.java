package com.reactivecommerce.notification.domain.port.in;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.model.NotificationType;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Caso de uso principal: crear una notificación, persistirla,
 * entregarla vía SSE y opcionalmente enviar email.
 * Invocado desde el NotificationKafkaConsumer al recibir cada evento de dominio.
 */
public interface SendNotificationUseCase {
    record Command(
        UUID userId,
        NotificationType type,
        String title,
        String message,
        boolean sendEmail,
        String emailTemplate,
        String recipientEmail
    ) {}
    Mono<Notification> execute(Command command);
}
