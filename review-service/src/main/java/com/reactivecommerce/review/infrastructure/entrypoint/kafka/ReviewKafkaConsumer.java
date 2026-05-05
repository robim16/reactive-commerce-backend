package com.reactivecommerce.review.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Map;

/**
 * Consumer reactivo del Review Service.
 *
 * ── order.completed ───────────────────────────────────────────────────────
 *   Emitido por Order Service cuando un pago se completa.
 *   El Review Service lo registra internamente para validar que el buyer
 *   tiene derecho a dejar reseña (solo compradores verificados).
 *
 *   Nota: en esta implementación la validación de "compra verificada" se
 *   realiza en tiempo real en CreateReviewUseCaseImpl mediante una consulta
 *   al Order Service (vía WebClient, no implementada aquí para mantener
 *   el servicio desacoplado). El evento order.completed sirve como señal
 *   complementaria para un posible cache local de "buyers autorizados".
 *
 * ── order.refunded ────────────────────────────────────────────────────────
 *   Emitido por Order Service cuando se aprueba un reembolso.
 *   Decisión de negocio actual: la reseña del buyer NO se elimina ni se oculta
 *   automáticamente al reembolsar. El moderador puede ocultarla manualmente
 *   si lo considera apropiado.
 *   Este handler loguea el evento para auditoría y deja la lógica lista para
 *   extenderse sin cambiar la interfaz del use case.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewKafkaConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper                  objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError(e -> log.error("Kafka error in review-service: {}", e.getMessage()))
            .retry()
            .subscribe();
    }

    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(
                record.value(), new TypeReference<Map<String, Object>>() {}))
            .flatMap(payload -> switch (record.topic()) {
                case "order.completed" -> handleOrderCompleted(payload);
                case "order.refunded"  -> handleOrderRefunded(payload);
                default -> {
                    log.warn("Unhandled topic in review-service: {}", record.topic());
                    yield Mono.<Void>empty();
                }
            })
            .doOnSuccess(v -> record.receiverOffset().acknowledge())
            .doOnError(e -> log.error("Error processing topic={} key={}: {}",
                record.topic(), record.key(), e.getMessage()))
            .onErrorResume(e -> {
                record.receiverOffset().acknowledge();
                return Mono.empty();
            });
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    /**
     * Registra que el buyer completó una compra y tiene derecho a dejar reseña.
     * Payload esperado: { orderId, assetId, buyerId, amount, createdAt }
     *
     * En producción: persistir en colección verified_purchases para que
     * CreateReviewUseCaseImpl valide localmente sin llamada síncrona al Order Service.
     */
    private Mono<Void> handleOrderCompleted(Map<String, Object> payload) {
        String orderId = payload.getOrDefault("orderId", "unknown").toString();
        String assetId = payload.getOrDefault("assetId", "unknown").toString();
        String buyerId = payload.getOrDefault("buyerId", "unknown").toString();
        log.info("Verified purchase registered: orderId={} assetId={} buyerId={}",
            orderId, assetId, buyerId);
        // TODO (producción): persistir en colección verified_purchases con
        //   { buyerId, assetId, orderId, completedAt }
        //   para validación local en CreateReviewUseCaseImpl sin llamada HTTP
        return Mono.empty();
    }

    /**
     * Registra un reembolso para auditoría.
     * La reseña NO se oculta automáticamente (política de la plataforma).
     * Payload esperado: { orderId, assetId, buyerId, reason, refundedAt }
     */
    private Mono<Void> handleOrderRefunded(Map<String, Object> payload) {
        String orderId = payload.getOrDefault("orderId", "unknown").toString();
        String assetId = payload.getOrDefault("assetId", "unknown").toString();
        String buyerId = payload.getOrDefault("buyerId", "unknown").toString();
        log.info("Refund registered (review kept): orderId={} assetId={} buyerId={}",
            orderId, assetId, buyerId);
        // Política actual: no ocultar la reseña automáticamente.
        // Si se cambia, llamar a moderateReviewUseCase.hide() aquí.
        return Mono.empty();
    }
}
