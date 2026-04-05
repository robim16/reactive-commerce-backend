package com.reactivecommerce.order.domain.model;

import lombok.Builder;
import lombok.With;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
public record Order(
    UUID id,
    UUID buyerId,
    UUID assetId,
    BigDecimal amount,
    OrderStatus status,
    String failureReason,
    String transactionId,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt
) {
    public static Order create(UUID buyerId, UUID assetId, BigDecimal amount) {
        Instant now = Instant.now();
        return Order.builder()
            .id(UUID.randomUUID())
            .buyerId(buyerId).assetId(assetId).amount(amount)
            .status(OrderStatus.PENDING)
            .createdAt(now).updatedAt(now)
            .expiresAt(now.plusSeconds(900)) // 15 min TTL
            .build();
    }

    public Order confirm(String transactionId) {
        return this.withStatus(OrderStatus.COMPLETED)
            .withTransactionId(transactionId)
            .withUpdatedAt(Instant.now());
    }

    public Order fail(String reason) {
        return this.withStatus(OrderStatus.FAILED)
            .withFailureReason(reason)
            .withUpdatedAt(Instant.now());
    }

    public Order refund() {
        return this.withStatus(OrderStatus.REFUNDED).withUpdatedAt(Instant.now());
    }
}
