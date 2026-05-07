package com.reactivecommerce.order.infrastructure.adapter.persistence;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de persistencia para Order.
 *
 * Usa R2dbcEntityTemplate con insert() y update() explícitos
 * para evitar el bug de Spring Data R2DBC donde save() con UUID
 * no-nulo emite UPDATE en lugar de INSERT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final R2dbcEntityTemplate  template;
    private final OrderR2dbcRepository r2dbcRepository;

    @Override
    public Mono<Order> save(Order order) {
        return template.insert(toEntity(order))
            .map(this::toDomain)
            .doOnSuccess(o -> log.debug("Order inserted: id={} buyerId={}", o.id(), o.buyerId()));
    }

    @Override
    public Mono<Order> update(Order order) {
        return template.update(
                Query.query(Criteria.where("id").is(order.id())),
                Update.update("status",         order.status() != null ? order.status().name() : null)
                      .set("failure_reason",    order.failureReason())
                      .set("transaction_id",    order.transactionId())
                      .set("updated_at",        Instant.now()),
                OrderEntity.class
            )
            .flatMap(count -> {
                if (count == 0) {
                    return Mono.error(new IllegalStateException(
                        "Order not found for update: " + order.id()));
                }
                return findById(order.id());
            })
            .doOnSuccess(o -> log.debug("Order updated: id={} status={}", o.id(), o.status()));
    }

    @Override
    public Mono<Order> findById(UUID id) {
        return r2dbcRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<Order> findByBuyerId(UUID buyerId) {
        return r2dbcRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).map(this::toDomain);
    }

    @Override
    public Flux<Order> findByBuyerIdAndStatus(UUID buyerId, OrderStatus status) {
        return r2dbcRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, status)
            .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByBuyerIdAndAssetId(UUID buyerId, UUID assetId) {
        return r2dbcRepository.existsByBuyerIdAndAssetId(buyerId, assetId);
    }

    @Override
    public Flux<Order> findExpiredPending() {
        return r2dbcRepository.findExpiredPending(Instant.now()).map(this::toDomain);
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private OrderEntity toEntity(Order o) {
        return OrderEntity.builder()
            .id(o.id())
            .buyerId(o.buyerId())
            .assetId(o.assetId())
            .amount(o.amount())
            .status(o.status())
            .failureReason(o.failureReason())
            .transactionId(o.transactionId())
            .createdAt(o.createdAt())
            .updatedAt(o.updatedAt())
            .expiresAt(o.expiresAt())
            .build();
    }

    private Order toDomain(OrderEntity e) {
        return Order.builder()
            .id(e.getId())
            .buyerId(e.getBuyerId())
            .assetId(e.getAssetId())
            .amount(e.getAmount())
            .status(e.getStatus())
            .failureReason(e.getFailureReason())
            .transactionId(e.getTransactionId())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .expiresAt(e.getExpiresAt())
            .build();
    }
}
