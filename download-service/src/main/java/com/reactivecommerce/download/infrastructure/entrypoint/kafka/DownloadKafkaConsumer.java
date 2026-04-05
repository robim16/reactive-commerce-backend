package com.reactivecommerce.download.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.download.domain.port.in.GenerateDownloadTokenUseCase;
import com.reactivecommerce.download.domain.port.in.RevokeTokenUseCase;
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
 * Consumer reactivo del Download Service.
 * - order.completed   → genera token de descarga
 * - order.refunded    → revoca el token (takeUntilOther pattern)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadKafkaConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final GenerateDownloadTokenUseCase generateTokenUseCase;
    private final RevokeTokenUseCase revokeTokenUseCase;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError(e -> log.error("Kafka error in download-service", e))
            .retry()
            .subscribe();
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(record.value(), Map.class))
            .flatMap(payload -> switch (record.topic()) {
                case "order.completed" -> handleOrderCompleted(payload);
                case "order.refunded"  -> handleOrderRefunded(payload);
                default -> Mono.empty();
            })
            .doOnSuccess(v -> record.receiverOffset().acknowledge())
            .onErrorResume(e -> {
                log.error("Error in download consumer", e);
                return Mono.empty();
            });
    }

    private Mono<Void> handleOrderCompleted(Map<String, Object> payload) {
        UUID orderId  = UUID.fromString(payload.get("orderId").toString());
        UUID buyerId  = UUID.fromString(payload.get("buyerId").toString());
        UUID assetId  = UUID.fromString(payload.get("assetId").toString());
        String s3Key  = "assets/" + assetId + "/original";

        return generateTokenUseCase.execute(
            new GenerateDownloadTokenUseCase.Command(orderId, buyerId, assetId, s3Key))
            .doOnSuccess(t -> log.info("Download token created for order {}", orderId))
            .then();
    }

    private Mono<Void> handleOrderRefunded(Map<String, Object> payload) {
        UUID orderId = UUID.fromString(payload.get("orderId").toString());
        return revokeTokenUseCase.execute(orderId)
            .doOnSuccess(v -> log.info("Download token revoked for order {}", orderId));
    }
}
