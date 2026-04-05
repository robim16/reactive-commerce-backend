package com.reactivecommerce.order.infrastructure.entrypoint.web;

import com.reactivecommerce.order.domain.model.OrderStatus;
import com.reactivecommerce.order.domain.port.in.*;
import com.reactivecommerce.order.infrastructure.entrypoint.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler reactivo del Order Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderHandler {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;
    private final RefundOrderUseCase refundOrderUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        UUID buyerId = extractUserId(request);
        return request.bodyToMono(CreateOrderRequest.class)
            .flatMap(body -> createOrderUseCase.execute(
                new CreateOrderUseCase.Command(buyerId, UUID.fromString(body.assetId()))))
            .flatMap(order -> ServerResponse.status(HttpStatus.CREATED)
                .bodyValue(OrderResponse.from(order)))
            .onErrorResume(IllegalStateException.class, e ->
                e.getMessage().contains("Ya tienes")
                    ? ServerResponse.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .bodyValue(ErrorResponse.of(e.getMessage()))
                    : ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        UUID buyerId = extractUserId(request);
        OrderStatus status = request.queryParam("status")
            .map(OrderStatus::valueOf).orElse(null);
        String cursor = request.queryParam("cursor").orElse(null);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(20);

        return ServerResponse.ok()
            .body(getOrdersUseCase.findByBuyer(buyerId, status, cursor, size)
                .map(OrderResponse::from), OrderResponse.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        UUID orderId = UUID.fromString(request.pathVariable("id"));
        return getOrdersUseCase.findById(orderId)
            .flatMap(order -> ServerResponse.ok().bodyValue(OrderResponse.from(order)))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> refund(ServerRequest request) {
        UUID buyerId = extractUserId(request);
        UUID orderId = UUID.fromString(request.pathVariable("id"));
        return request.bodyToMono(RefundRequest.class)
            .flatMap(body -> refundOrderUseCase.execute(
                new RefundOrderUseCase.Command(orderId, buyerId, body.reason())))
            .flatMap(order -> ServerResponse.ok().bodyValue(OrderResponse.from(order)))
            .onErrorResume(IllegalStateException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    private UUID extractUserId(ServerRequest request) {
        return UUID.fromString(request.headers().firstHeader("X-User-Id"));
    }
}
