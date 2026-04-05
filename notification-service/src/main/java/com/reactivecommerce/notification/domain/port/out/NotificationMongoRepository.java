package com.reactivecommerce.notification.domain.port.out;

import com.reactivecommerce.notification.domain.model.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

/**
 * Repositorio Spring Data MongoDB Reactive.
 * Extiende ReactiveMongoRepository para operaciones CRUD básicas.
 * Las consultas personalizadas se delegan al adapter.
 */
public interface NotificationMongoRepository
    extends ReactiveMongoRepository<Notification, String> {

    Flux<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);
    Flux<Notification> findByUserId(UUID userId);
}
