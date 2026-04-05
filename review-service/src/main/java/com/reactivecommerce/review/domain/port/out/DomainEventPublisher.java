package com.reactivecommerce.review.domain.port.out;
import reactor.core.publisher.Mono;
public interface DomainEventPublisher {
    Mono<Void> publish(String topic, String key, Object event);
}
