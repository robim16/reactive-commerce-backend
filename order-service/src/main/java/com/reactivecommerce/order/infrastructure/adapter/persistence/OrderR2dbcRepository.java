package com.reactivecommerce.order.infrastructure.adapter.persistence;

import com.reactivecommerce.order.domain.model.OrderStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface OrderR2dbcRepository extends ReactiveCrudRepository<OrderEntity, UUID> {
    Flux<OrderEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
    Flux<OrderEntity> findByBuyerIdAndStatusOrderByCreatedAtDesc(UUID buyerId, OrderStatus status);
    Mono<Boolean> existsByBuyerIdAndAssetId(UUID buyerId, UUID assetId);

    @Query("SELECT * FROM orders WHERE status = 'PENDING' AND expires_at < :now")
    Flux<OrderEntity> findExpiredPending(Instant now);
}
