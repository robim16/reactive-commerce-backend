package com.reactivecommerce.order.domain.port.in;

import com.reactivecommerce.order.domain.model.Order;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ConfirmOrderUseCase {
    record Command(UUID orderId, String transactionId) {}
    Mono<Order> execute(Command command);
}
