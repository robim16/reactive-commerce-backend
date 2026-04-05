package com.reactivecommerce.payment.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.payment.domain.port.in.ProcessPaymentUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumer reactivo del Payment Service.
 * Escucha order.created para iniciar el procesamiento del pago (Saga).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError(e -> log.error("Kafka error in payment-service: {}", e.getMessage()))
            .retry()
            .subscribe();
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(record.value(), Map.class))
            .flatMap(payload -> {
                UUID orderId = UUID.fromString(payload.get("orderId").toString());
                UUID buyerId = UUID.fromString(payload.get("buyerId").toString());
                BigDecimal amount = new BigDecimal(payload.get("amount").toString());
                return processPaymentUseCase.execute(
                    new ProcessPaymentUseCase.Command(orderId, amount, buyerId));
            })
            .doOnSuccess(tx -> record.receiverOffset().acknowledge())
            .doOnError(e -> log.error("Payment processing failed: {}", e.getMessage()))
            .onErrorResume(e -> Mono.empty())
            .then();
    }
}
