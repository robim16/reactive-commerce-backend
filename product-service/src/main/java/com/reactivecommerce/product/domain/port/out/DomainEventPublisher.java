package com.reactivecommerce.product.domain.port.out;

import reactor.core.publisher.Mono;

public interface DomainEventPublisher {
    Mono<Void> publish(String topic, String key, Object event);
}
