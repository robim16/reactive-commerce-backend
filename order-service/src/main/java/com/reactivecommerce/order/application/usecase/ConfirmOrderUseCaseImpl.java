package com.reactivecommerce.order.application.usecase;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.port.in.ConfirmOrderUseCase;
import com.reactivecommerce.order.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfirmOrderUseCaseImpl implements ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Order> execute(Command command) {
        return orderRepository.findById(command.orderId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found")))
            .map(order -> order.confirm(command.transactionId()))
            .flatMap(orderRepository::update)
            .flatMap(order -> eventPublisher.publish("order.completed", order.id().toString(),
                    Map.of("orderId", order.id(), "buyerId", order.buyerId(),
                           "assetId", order.assetId(), "transactionId", order.transactionId(),
                           "amount", order.amount()))
                .thenReturn(order));
    }
}
