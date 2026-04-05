package com.reactivecommerce.order.application.usecase;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.in.RefundOrderUseCase;
import com.reactivecommerce.order.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundOrderUseCaseImpl implements RefundOrderUseCase {

    private static final int REFUND_WINDOW_DAYS = 7;

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Order> execute(Command command) {
        return orderRepository.findById(command.orderId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found")))
            .flatMap(order -> {
                if (!order.buyerId().equals(command.buyerId())) {
                    return Mono.error(new IllegalStateException("Unauthorized"));
                }
                if (order.status() != OrderStatus.COMPLETED) {
                    return Mono.error(new IllegalStateException("Only completed orders can be refunded"));
                }
                long daysSincePurchase = ChronoUnit.DAYS.between(order.createdAt(), Instant.now());
                if (daysSincePurchase > REFUND_WINDOW_DAYS) {
                    return Mono.error(new IllegalStateException("Refund window has expired (7 days)"));
                }
                return Mono.just(order.refund());
            })
            .flatMap(orderRepository::update)
            .flatMap(order -> eventPublisher.publish("order.refunded", order.id().toString(),
                    Map.of("orderId", order.id(), "buyerId", order.buyerId(),
                           "assetId", order.assetId(), "reason", command.reason()))
                .thenReturn(order));
    }
}
