package com.reactivecommerce.review.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.review.domain.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Adaptador Kafka que implementa DomainEventPublisher.
 *
 * Publica los eventos de dominio del Review Service:
 *   review.created  → Order Service puede verificar si la reseña ya existía;
 *                     Notification Service notifica al creator del asset.
 *   review.hidden   → Notification Service informa al creator de la moderación.
 *   review.restored → Notification Service informa que la reseña fue restituida.
 *
 * ACKS=all + idempotencia garantizan exactly-once en la publicación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements DomainEventPublisher {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper                objectMapper;

    @Override
    public Mono<Void> publish(String topic, String key, Object event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
            .map(payload -> SenderRecord.create(
                new ProducerRecord<>(topic, key, payload), key))
                .as(record -> kafkaSender.send(Mono.just(record).flatMap(emitted -> emitted)))
            .next()
            .doOnSuccess(r -> log.info("Event published → topic={} key={}", topic, key))
            .doOnError(e -> log.error("Failed to publish → topic={} key={} error={}",
                topic, key, e.getMessage()))
            .then();
    }
}
