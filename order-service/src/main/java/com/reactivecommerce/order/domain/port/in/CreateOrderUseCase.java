package com.reactivecommerce.order.domain.port.in;

import com.reactivecommerce.order.domain.model.Order;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface CreateOrderUseCase {
    record Command(UUID buyerId, UUID assetId) {}
    Mono<Order> execute(Command command);
}
