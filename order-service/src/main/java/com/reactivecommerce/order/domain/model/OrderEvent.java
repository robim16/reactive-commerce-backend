package com.reactivecommerce.order.domain.model;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Builder
public record OrderEvent(
    UUID id,
    UUID orderId,
    String eventType,
    String payload,
    Instant occurredAt
) {}
