package com.reactivecommerce.payment.domain.port.out;

import com.reactivecommerce.payment.domain.model.Transaction;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface TransactionRepository {
    Mono<Transaction> save(Transaction transaction);
    Mono<Transaction> findByOrderId(UUID orderId);
    Mono<Transaction> update(Transaction transaction);
}
