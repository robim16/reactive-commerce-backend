package com.reactivecommerce.order.domain.port.in;

import com.reactivecommerce.order.domain.model.Order;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface FailOrderUseCase {
    record Command(UUID orderId, String reason) {}
    Mono<Order> execute(Command command);
}
