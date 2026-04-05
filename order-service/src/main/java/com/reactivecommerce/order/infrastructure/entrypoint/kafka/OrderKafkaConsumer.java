package com.reactivecommerce.order.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.order.domain.port.in.ConfirmOrderUseCase;
import com.reactivecommerce.order.domain.port.in.FailOrderUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Map;
import java.util.UUID;

/**
 * Consumer reactivo del Order Service.
 * Participa en la Saga de compra como coreógrafo:
 *  - order.payment_completed → confirma el pedido
 *  - order.payment_failed    → marca el pedido como fallido
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final FailOrderUseCase failOrderUseCase;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError(e -> log.error("Kafka error in order-service: {}", e.getMessage()))
            .retry()
            .subscribe();
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(record.value(), Map.class))
            .flatMap(payload -> switch (record.topic()) {
                case "order.payment_completed" -> handlePaymentCompleted(payload);
                case "order.payment_failed"    -> handlePaymentFailed(payload);
                default -> Mono.empty();
            })
            .doOnSuccess(v -> record.receiverOffset().acknowledge())
            .onErrorResume(e -> {
                log.error("Error processing {}: {}", record.topic(), e);
                return Mono.empty();
            });
    }

    private Mono<Void> handlePaymentCompleted(Map<String, Object> payload) {
        UUID orderId = UUID.fromString(payload.get("orderId").toString());
        String transactionId = payload.get("transactionId").toString();
        return confirmOrderUseCase.execute(new ConfirmOrderUseCase.Command(orderId, transactionId))
            .doOnSuccess(o -> log.info("Order confirmed: {}", orderId))
            .then();
    }

    private Mono<Void> handlePaymentFailed(Map<String, Object> payload) {
        UUID orderId = UUID.fromString(payload.get("orderId").toString());
        String reason = payload.getOrDefault("reason", "Payment failed").toString();
        return failOrderUseCase.execute(new FailOrderUseCase.Command(orderId, reason))
            .doOnSuccess(o -> log.info("Order failed: {}", orderId))
            .then();
    }
}
