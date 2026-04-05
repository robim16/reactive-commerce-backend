package com.reactivecommerce.notification.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.notification.domain.model.NotificationType;
import com.reactivecommerce.notification.domain.port.in.SendNotificationUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Consumer reactivo del Notification Service.
 *
 * Transforma cada evento de dominio Kafka en un Command para SendNotificationUseCase.
 * Toda la lógica de persistencia, SSE y email vive en el use case — aquí solo
 * se hace el mapeo tópico → comando.
 *
 * No accede directamente a ningún puerto de salida (NotificationRepository,
 * SsePublisherPort, EmailPort): eso viola la arquitectura hexagonal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private static final Set<String> EMAIL_TOPICS = Set.of(
        "order.completed", "asset.approved", "asset.rejected",
        "order.refunded",  "user.registered"
    );

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError(e -> log.error("Kafka error in notification-service: {}", e.getMessage()))
            .retry()
            .subscribe();
    }

    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(record.value(),
            new TypeReference<Map<String, Object>>() {}))
            .flatMap(payload -> buildCommand(record.topic(), payload))
            .flatMap(sendNotificationUseCase::execute)
            .doOnSuccess(n -> record.receiverOffset().acknowledge())
            .doOnError(e -> log.error("Error processing topic={}: {}", record.topic(), e.getMessage()))
            .onErrorResume(e -> Mono.empty())
            .then();
    }

    private Mono<SendNotificationUseCase.Command> buildCommand(
            String topic, Map<String, Object> payload) {

        return Mono.fromCallable(() -> switch (topic) {

            case "order.created" -> new SendNotificationUseCase.Command(
                uuid(payload, "buyerId"),
                NotificationType.ORDER_CREATED,
                "Pedido iniciado",
                "Tu pedido est\u00e1 siendo procesado.",
                false, null, null);

            case "order.completed" -> new SendNotificationUseCase.Command(
                uuid(payload, "buyerId"),
                NotificationType.ORDER_COMPLETED,
                "\u00a1Compra exitosa!",
                "Tu asset est\u00e1 listo para descargar.",
                true, "order-completed", resolveEmail(payload, "buyerId"));

            case "order.payment_failed" -> new SendNotificationUseCase.Command(
                uuid(payload, "buyerId"),
                NotificationType.ORDER_FAILED,
                "Pago fallido",
                "No pudimos procesar tu pago. Intenta de nuevo.",
                false, null, null);

            case "order.refunded" -> new SendNotificationUseCase.Command(
                uuid(payload, "buyerId"),
                NotificationType.ORDER_REFUNDED,
                "Reembolso procesado",
                "Tu reembolso ha sido procesado exitosamente.",
                true, "order-refunded", resolveEmail(payload, "buyerId"));

            case "asset.uploaded" -> new SendNotificationUseCase.Command(
                uuid(payload, "creatorId"),
                NotificationType.ASSET_UPLOADED,
                "Asset recibido",
                "Tu asset est\u00e1 en revisi\u00f3n de moderaci\u00f3n.",
                false, null, null);

            case "asset.approved" -> new SendNotificationUseCase.Command(
                uuid(payload, "creatorId"),
                NotificationType.ASSET_APPROVED,
                "Asset aprobado",
                "Tu asset fue aprobado y puede ser publicado.",
                true, "asset-approved", resolveEmail(payload, "creatorId"));

            case "asset.rejected" -> new SendNotificationUseCase.Command(
                uuid(payload, "creatorId"),
                NotificationType.ASSET_REJECTED,
                "Asset rechazado",
                "Tu asset fue rechazado. Motivo: "
                    + payload.getOrDefault("reason", "Sin especificar"),
                true, "asset-rejected", resolveEmail(payload, "creatorId"));

            case "asset.published" -> new SendNotificationUseCase.Command(
                uuid(payload, "creatorId"),
                NotificationType.ASSET_PUBLISHED,
                "Asset publicado",
                "Tu asset ya est\u00e1 disponible en el marketplace.",
                false, null, null);

            case "review.created" -> new SendNotificationUseCase.Command(
                uuid(payload, "creatorId"),
                NotificationType.REVIEW_CREATED,
                "Nueva valoraci\u00f3n",
                "Un comprador ha dejado una valoraci\u00f3n en tu asset.",
                false, null, null);

            case "user.registered" -> new SendNotificationUseCase.Command(
                uuid(payload, "userId"),
                NotificationType.WELCOME,
                "\u00a1Bienvenido a ReactiveCommerce!",
                "Tu cuenta ha sido creada. Verifica tu email para comenzar.",
                true, "user-registered", payload.get("email").toString());

            default -> throw new IllegalArgumentException("Unhandled topic: " + topic);
        });
    }

    private UUID uuid(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) throw new IllegalArgumentException("Missing key: " + key);
        return UUID.fromString(val.toString());
    }

    /**
     * En produccion el email del usuario se resolveria consultando el Auth Service
     * o incluyendolo en el payload del evento. Por ahora se usa un placeholder
     * que debe sustituirse por la integracion real.
     */
    private String resolveEmail(Map<String, Object> payload, String userIdKey) {
        return payload.getOrDefault("email",
            "user+" + payload.get(userIdKey) + "@reactivecommerce.com").toString();
    }
}
