package com.reactivecommerce.order.application.usecase;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.in.FailOrderUseCase;
import com.reactivecommerce.order.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Implementación del caso de uso de fallo de pedido.
 *
 * Invocado desde OrderKafkaConsumer al consumir order.payment_failed.
 * También lo usa ExpiredOrderJob para cancelar pedidos con TTL vencido.
 *
 * Flujo:
 *  1. Carga el pedido por ID.
 *  2. Valida que esté en estado PENDING (idempotente si ya está FAILED).
 *  3. Marca como FAILED con el motivo recibido del Payment Service.
 *  4. Publica order.payment_failed para que Notification Service alerte al buyer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailOrderUseCaseImpl implements FailOrderUseCase {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Order> execute(Command command) {
        return orderRepository.findById(command.orderId())
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Order not found: " + command.orderId())))
            .flatMap(order -> {
                // Idempotencia: si ya está en estado terminal no hacemos nada
                if (order.status() == OrderStatus.FAILED
                        || order.status() == OrderStatus.CANCELLED) {
                    log.debug("Order {} already in terminal state {}, skipping fail",
                        command.orderId(), order.status());
                    return Mono.just(order);
                }
                return orderRepository.update(order.fail(command.reason()))
                    .flatMap(failed -> eventPublisher.publish(
                            "order.payment_failed",
                            failed.id().toString(),
                            Map.of(
                                "orderId", failed.id(),
                                "buyerId", failed.buyerId(),
                                "reason",  command.reason()
                            ))
                        .thenReturn(failed));
            })
            .doOnSuccess(o -> log.info("Order failed: id={} reason={}", o.id(), command.reason()));
    }
}
