package com.reactivecommerce.payment.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.payment.domain.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para Transaction.
 * No expone datos sensibles — solo el resultado del pago.
 */
public record TransactionResponse(
    UUID id,
    UUID orderId,
    BigDecimal amount,
    BigDecimal platformCommission,
    BigDecimal creatorAmount,
    String status,
    String gatewayTransactionId,
    String failureCode,
    Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
            t.id(),
            t.orderId(),
            t.amount(),
            t.platformCommission(),
            t.creatorAmount(),
            t.status().name(),
            t.gatewayTransactionId(),
            t.failureCode(),
            t.createdAt()
        );
    }
}
