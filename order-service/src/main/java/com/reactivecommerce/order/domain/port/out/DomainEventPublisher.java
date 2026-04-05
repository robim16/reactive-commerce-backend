package com.reactivecommerce.order.domain.port.out;

import reactor.core.publisher.Mono;

public interface DomainEventPublisher {
    Mono<Void> publish(String topic, String key, Object event);
}
