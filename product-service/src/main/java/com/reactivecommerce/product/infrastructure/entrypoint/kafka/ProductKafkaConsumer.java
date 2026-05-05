package com.reactivecommerce.product.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
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
 * Consumer reactivo del Product Service.
 * Escucha review.created para actualizar el rating promedio del asset.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError((Throwable e) -> log.error("Error consuming Kafka message: {}", e.getMessage()))
            .retry()
            .subscribe();
    }

    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(
                record.value(), new TypeReference<Map<String, Object>>() {}))
            .flatMap(payload -> {
                String topic = record.topic();
                log.info("Received event on topic={} key={}", topic, record.key());
                return switch (topic) {
                    case "review.created" -> handleReviewCreated(payload);
                    default -> Mono.empty();
                };
            })
            .doOnSuccess(v -> record.receiverOffset().acknowledge())
            .doOnError(e -> log.error("Failed to process record: {}", e.getMessage()))
            .onErrorResume(e -> Mono.empty());
    }

    private Mono<Void> handleReviewCreated(Map<String, Object> payload) {
        UUID assetId = UUID.fromString(payload.get("assetId").toString());
        double newRating = Double.parseDouble(payload.get("averageRating").toString());
        int totalReviews = Integer.parseInt(payload.get("totalReviews").toString());

        return assetRepository.findById(assetId)
            .flatMap(asset -> assetRepository.update(
                asset.withAverageRating(newRating).withTotalReviews(totalReviews)))
            .doOnSuccess(a -> log.info("Updated rating for asset {}: {}", assetId, newRating))
            .then();
    }
}
