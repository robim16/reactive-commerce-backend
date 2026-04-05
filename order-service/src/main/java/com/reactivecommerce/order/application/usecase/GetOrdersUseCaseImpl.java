package com.reactivecommerce.order.application.usecase;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.in.GetOrdersUseCase;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Implementación del caso de uso de consulta de pedidos.
 *
 * Invocado desde OrderHandler en:
 *   GET /api/v1/orders          → findByBuyer (historial del buyer)
 *   GET /api/v1/orders/{id}     → findById (detalle de un pedido)
 *
 * La paginación con cursor se aplica en findByBuyer:
 *   - Si cursor == null → primera página (los más recientes)
 *   - Si cursor != null → pedidos anteriores al cursor (createdAt < cursor)
 * El cursor es el createdAt ISO del último ítem de la página anterior,
 * lo que garantiza estabilidad aunque se inserten nuevos pedidos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrdersUseCaseImpl implements GetOrdersUseCase {

    private final OrderRepository orderRepository;

    @Override
    public Flux<Order> findByBuyer(UUID buyerId, OrderStatus status, String cursor, int size) {
        Flux<Order> base = (status != null)
            ? orderRepository.findByBuyerIdAndStatus(buyerId, status)
            : orderRepository.findByBuyerId(buyerId);

        return base
            // Cursor-based pagination: skip orders newer than cursor
            .skipWhile(order -> cursor != null
                && order.createdAt().toString().compareTo(cursor) >= 0)
            .take(size)
            .doOnComplete(() -> log.debug(
                "Orders fetched for buyerId={} status={} size={}", buyerId, status, size));
    }

    @Override
    public Mono<Order> findById(UUID orderId) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Order not found: " + orderId)));
    }
}
