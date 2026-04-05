package com.reactivecommerce.payment.infrastructure.entrypoint.web;

import com.reactivecommerce.payment.domain.port.in.GetTransactionUseCase;
import com.reactivecommerce.payment.infrastructure.entrypoint.web.dto.ErrorResponse;
import com.reactivecommerce.payment.infrastructure.entrypoint.web.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler reactivo del Payment Service.
 * Delega a GetTransactionUseCase — no accede directamente a ningún puerto out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentHandler {

    private final GetTransactionUseCase getTransactionUseCase;

    /**
     * GET /api/v1/payments/orders/{orderId}
     * Devuelve el estado de la transacción de una orden.
     * Usado por el frontend para mostrar el resultado del pago.
     */
    public Mono<ServerResponse> getByOrder(ServerRequest request) {
        UUID orderId = UUID.fromString(request.pathVariable("orderId"));
        return getTransactionUseCase.findByOrderId(orderId)
            .map(TransactionResponse::from)
            .flatMap(tx -> ServerResponse.ok().bodyValue(tx))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.notFound().build())
            .onErrorResume(e -> {
                log.error("Error fetching transaction for orderId={}: {}", orderId, e.getMessage());
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyValue(ErrorResponse.of("Error al obtener la transacción"));
            });
    }
}
