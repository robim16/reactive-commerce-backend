package com.reactivecommerce.notification.domain.port.out;

import com.reactivecommerce.notification.domain.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NotificationRepository {
    Mono<Notification> save(Notification notification);
    Flux<Notification> findUnreadByUserId(UUID userId, int limit);
    Mono<Void> markAllReadByUserId(UUID userId);
    Mono<Void> markReadById(String id);
}
