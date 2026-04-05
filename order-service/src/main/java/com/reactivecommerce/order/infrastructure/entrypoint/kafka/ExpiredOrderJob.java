package com.reactivecommerce.order.infrastructure.entrypoint.kafka;

import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * Job reactivo usando Flux.interval.
 * Cancela pedidos PENDING que superaron su TTL de 15 minutos.
 * Patrón mencionado en la spec: Flux.interval para verificación periódica.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredOrderJob {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @PostConstruct
    public void scheduleExpirationCheck() {
        Flux.interval(Duration.ofMinutes(5))
            .flatMap(tick -> orderRepository.findExpiredPending()
                .flatMap(order -> orderRepository.update(
                    order.withStatus(OrderStatus.CANCELLED))
                    .flatMap(cancelled -> eventPublisher.publish(
                        "order.payment_failed", cancelled.id().toString(),
                        Map.of("orderId", cancelled.id(), "reason", "Order expired")))
                )
            )
            .doOnError(e -> log.error("Error in expiration job: {}", e.getMessage()))
            .retry()
            .subscribe();
    }
}
