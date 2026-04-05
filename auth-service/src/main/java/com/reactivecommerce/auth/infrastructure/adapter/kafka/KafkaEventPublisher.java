package com.reactivecommerce.auth.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.auth.domain.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements DomainEventPublisher {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publish(String topic, String key, Object event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
            .map(payload -> SenderRecord.create(
                new ProducerRecord<>(topic, key, payload), key))
            .as(record -> kafkaSender.send(Mono.just(record).flatMap(emitted -> emitted)))
            .next()
            .doOnSuccess(r -> log.info("Event published to {} key={}", topic, key))
            .doOnError(e -> log.error("Failed to publish to {}: {}", topic, e.getMessage()))
            .then();
    }
}
