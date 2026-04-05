package com.reactivecommerce.payment.domain.model;

import lombok.Builder;

import java.awt.image.PixelGrabber;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record Transaction(
    UUID id,
    UUID orderId,
    BigDecimal amount,
    BigDecimal platformCommission,
    BigDecimal creatorAmount,
    TransactionStatus status,
    String gatewayTransactionId,
    String failureCode,
    String failureMessage,
    Instant createdAt
) {
    public static Transaction create(UUID orderId, BigDecimal amount, BigDecimal commissionRate) {
        BigDecimal commission = amount.multiply(commissionRate);
        return Transaction.builder()
            .id(UUID.randomUUID())
            .orderId(orderId).amount(amount)
            .platformCommission(commission)
            .creatorAmount(amount.subtract(commission))
            .status(TransactionStatus.PENDING)
            .createdAt(Instant.now())
            .build();
    }
}
