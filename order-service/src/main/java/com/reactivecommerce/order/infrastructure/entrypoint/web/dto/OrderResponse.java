package com.reactivecommerce.order.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.order.domain.model.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(UUID id, UUID buyerId, UUID assetId, BigDecimal amount,
                             String status, String transactionId, Instant createdAt) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(o.id(), o.buyerId(), o.assetId(), o.amount(),
            o.status().name(), o.transactionId(), o.createdAt());
    }
}
