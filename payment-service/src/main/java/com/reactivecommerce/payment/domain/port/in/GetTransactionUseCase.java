package com.reactivecommerce.payment.domain.port.in;

import com.reactivecommerce.payment.domain.model.Transaction;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Caso de uso de consulta: obtener la transacción de una orden.
 * Invocado desde PaymentHandler en GET /api/v1/payments/orders/{orderId}.
 */
public interface GetTransactionUseCase {
    Mono<Transaction> findByOrderId(UUID orderId);
}
