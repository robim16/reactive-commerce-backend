package com.reactivecommerce.payment.domain.port.out;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayPort {
    record GatewayResult(String transactionId, boolean success, String failureCode) {}
    Mono<GatewayResult> charge(UUID buyerId, BigDecimal amount, String idempotencyKey);
}
