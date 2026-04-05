package com.reactivecommerce.notification.application.usecase;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.port.in.SendNotificationUseCase;
import com.reactivecommerce.notification.domain.port.out.EmailPort;
import com.reactivecommerce.notification.domain.port.out.NotificationRepository;
import com.reactivecommerce.notification.domain.port.out.SsePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Orquesta el flujo completo de entrega de una notificación:
 *  1. Persiste en MongoDB (para recuperación offline y historial).
 *  2. Empuja al sink SSE del usuario si está conectado (entrega en tiempo real).
 *  3. Envía email vía SES si el comando lo solicita (eventos transaccionales).
 *
 * Los pasos 2 y 3 son fire-and-forget con manejo de error propio:
 * un fallo en SSE o email no debe revertir la persistencia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendNotificationUseCaseImpl implements SendNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final SsePublisherPort ssePublisherPort;
    private final EmailPort emailPort;

    @Override
    public Mono<Notification> execute(Command command) {
        Notification notification = Notification.create(
            command.userId(),
            command.type(),
            command.title(),
            command.message()
        );

        return notificationRepository.save(notification)
            .flatMap(saved ->
                // SSE: push al usuario si está conectado (no bloqueante, no falla el flujo)
                ssePublisherPort.push(saved.userId(), saved)
                    .onErrorResume(e -> {
                        log.warn("SSE push failed for userId={}: {}", saved.userId(), e.getMessage());
                        return Mono.empty();
                    })
                    .thenReturn(saved)
            )
            .flatMap(saved -> {
                if (!command.sendEmail() || command.recipientEmail() == null) {
                    return Mono.just(saved);
                }
                // Email: fire-and-forget, error no afecta el resultado
                return emailPort.send(
                        command.recipientEmail(),
                        command.emailTemplate(),
                        Map.of(
                            "userId",  saved.userId(),
                            "title",   saved.title(),
                            "message", saved.message()
                        )
                    )
                    .onErrorResume(e -> {
                        log.error("Email send failed to={}: {}", command.recipientEmail(), e.getMessage());
                        return Mono.empty();
                    })
                    .thenReturn(saved);
            })
            .doOnSuccess(n -> log.info("Notification delivered: type={} userId={}", n.type(), n.userId()));
    }
}
