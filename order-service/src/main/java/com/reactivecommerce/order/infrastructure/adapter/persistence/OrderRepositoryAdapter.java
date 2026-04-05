package com.reactivecommerce.order.infrastructure.adapter.persistence;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderR2dbcRepository r2dbcRepository;

    @Override public Mono<Order> save(Order o) {
        return r2dbcRepository.save(toEntity(o)).map(this::toDomain);
    }
    @Override public Mono<Order> findById(UUID id) {
        return r2dbcRepository.findById(id).map(this::toDomain);
    }
    @Override public Flux<Order> findByBuyerIdAndStatus(UUID buyerId, OrderStatus status) {
        return r2dbcRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, status).map(this::toDomain);
    }
    @Override public Flux<Order> findByBuyerId(UUID buyerId) {
        return r2dbcRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).map(this::toDomain);
    }
    @Override public Mono<Boolean> existsByBuyerIdAndAssetId(UUID buyerId, UUID assetId) {
        return r2dbcRepository.existsByBuyerIdAndAssetId(buyerId, assetId);
    }
    @Override public Mono<Order> update(Order o) {
        return r2dbcRepository.save(toEntity(o)).map(this::toDomain);
    }
    @Override public Flux<Order> findExpiredPending() {
        return r2dbcRepository.findExpiredPending(Instant.now()).map(this::toDomain);
    }

    private OrderEntity toEntity(Order o) {
        return OrderEntity.builder()
            .id(o.id()).buyerId(o.buyerId()).assetId(o.assetId()).amount(o.amount())
            .status(o.status()).failureReason(o.failureReason()).transactionId(o.transactionId())
            .createdAt(o.createdAt()).updatedAt(o.updatedAt()).expiresAt(o.expiresAt())
            .build();
    }

    private Order toDomain(OrderEntity e) {
        return Order.builder()
            .id(e.getId()).buyerId(e.getBuyerId()).assetId(e.getAssetId()).amount(e.getAmount())
            .status(e.getStatus()).failureReason(e.getFailureReason()).transactionId(e.getTransactionId())
            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).expiresAt(e.getExpiresAt())
            .build();
    }
}
