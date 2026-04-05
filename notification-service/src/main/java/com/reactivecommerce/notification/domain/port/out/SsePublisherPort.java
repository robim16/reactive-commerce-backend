package com.reactivecommerce.notification.domain.port.out;

import com.reactivecommerce.notification.domain.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface SsePublisherPort {
    Mono<Void> push(UUID userId, Notification notification);
    Flux<Notification> streamForUser(UUID userId);
}
