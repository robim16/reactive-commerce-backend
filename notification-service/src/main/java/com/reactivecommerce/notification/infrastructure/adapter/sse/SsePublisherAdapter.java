package com.reactivecommerce.notification.infrastructure.adapter.sse;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.port.out.SsePublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hot publisher SSE por usuario usando Sinks.Many.
 * Patrón: share() / publish() — múltiples suscriptores, un único Flux por usuario.
 */
@Slf4j
@Component
public class SsePublisherAdapter implements SsePublisherPort {

    private final Map<UUID, Sinks.Many<Notification>> userSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> push(UUID userId, Notification notification) {
        Sinks.Many<Notification> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(notification);
        }
        return Mono.empty();
    }

    @Override
    public Flux<Notification> streamForUser(UUID userId) {
        Sinks.Many<Notification> sink = userSinks.computeIfAbsent(userId,
            id -> Sinks.many().multicast().onBackpressureBuffer(256));

        return sink.asFlux()
            .doOnSubscribe(s -> log.info("SSE opened for user {}", userId))
            .doOnCancel(() -> {
                log.info("SSE closed for user {}", userId);
                userSinks.remove(userId);
            });
    }
}
