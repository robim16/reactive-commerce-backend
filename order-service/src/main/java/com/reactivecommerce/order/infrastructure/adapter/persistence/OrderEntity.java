package com.reactivecommerce.order.infrastructure.adapter.persistence;

import com.reactivecommerce.order.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("orders")
public class OrderEntity {
    @Id private UUID id;
    private UUID buyerId;
    private UUID assetId;
    private BigDecimal amount;
    private OrderStatus status;
    private String failureReason;
    private String transactionId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
}
