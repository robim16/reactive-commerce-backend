package com.reactivecommerce.order.domain.port.out;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface OrderRepository {
    Mono<Order> save(Order order);
    Mono<Order> findById(UUID id);
    Flux<Order> findByBuyerIdAndStatus(UUID buyerId, OrderStatus status);
    Flux<Order> findByBuyerId(UUID buyerId);
    Mono<Boolean> existsByBuyerIdAndAssetId(UUID buyerId, UUID assetId);
    Mono<Order> update(Order order);
    Flux<Order> findExpiredPending();
}
