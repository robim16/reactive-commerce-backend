package com.reactivecommerce.payment.infrastructure.adapter.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data MongoDB Reactive.
 * Spring genera la implementación en arranque vía proxy dinámico.
 */
public interface TransactionMongoRepository
        extends ReactiveMongoRepository<TransactionDocument, String> {

    Mono<TransactionDocument> findByOrderId(String orderId);
}
