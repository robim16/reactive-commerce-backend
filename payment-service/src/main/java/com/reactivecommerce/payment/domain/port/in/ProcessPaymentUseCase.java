package com.reactivecommerce.payment.domain.port.in;

import com.reactivecommerce.payment.domain.model.Transaction;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessPaymentUseCase {
    record Command(UUID orderId, BigDecimal amount, UUID buyerId) {}
    Mono<Transaction> execute(Command command);
}
