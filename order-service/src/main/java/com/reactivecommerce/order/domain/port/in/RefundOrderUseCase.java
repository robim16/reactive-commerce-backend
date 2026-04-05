package com.reactivecommerce.order.domain.port.in;

import com.reactivecommerce.order.domain.model.Order;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface RefundOrderUseCase {
    record Command(UUID orderId, UUID buyerId, String reason) {}
    Mono<Order> execute(Command command);
}
