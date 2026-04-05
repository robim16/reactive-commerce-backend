package com.reactivecommerce.order.domain.port.in;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface GetOrdersUseCase {
    Flux<Order> findByBuyer(UUID buyerId, OrderStatus status, String cursor, int size);
    Mono<Order> findById(UUID orderId);
}
