package com.reactivecommerce.notification.infrastructure.adapter.persistence;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.port.out.NotificationMongoRepository;
import com.reactivecommerce.notification.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adaptador MongoDB que implementa NotificationRepository (puerto de salida).
 *
 * Colección: notifications
 * Índices recomendados:
 *   { userId: 1, read: 1, createdAt: -1 }  → consulta findUnread
 *   { userId: 1 }                           → markAllRead
 *   { createdAt: 1 }, TTL: 90 días          → limpieza automática de notificaciones antiguas
 *
 * El dominio nunca conoce que el almacenamiento es MongoDB:
 * trabaja con el puerto NotificationRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationMongoRepository mongoRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<Notification> save(Notification notification) {
        return mongoRepository.save(notification)
            .doOnSuccess(n -> log.debug("Notification saved: id={} type={}", n.id(), n.type()));
    }

    @Override
    public Flux<Notification> findUnreadByUserId(UUID userId, int limit) {
        return mongoRepository
            .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
            .take(limit)
            .doOnComplete(() -> log.debug("Unread notifications fetched for userId={}", userId));
    }

    @Override
    public Mono<Void> markAllReadByUserId(UUID userId) {
        Query query = Query.query(
            Criteria.where("userId").is(userId).and("read").is(false)
        );
        Update update = Update.update("read", true);
        return mongoTemplate.updateMulti(query, update, Notification.class)
            .doOnSuccess(r -> log.debug("Marked {} notifications read for userId={}",
                r.getModifiedCount(), userId))
            .then();
    }

    @Override
    public Mono<Void> markReadById(String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = Update.update("read", true);
        return mongoTemplate.updateFirst(query, update, Notification.class)
            .doOnSuccess(r -> log.debug("Notification {} marked as read", id))
            .then();
    }
}
