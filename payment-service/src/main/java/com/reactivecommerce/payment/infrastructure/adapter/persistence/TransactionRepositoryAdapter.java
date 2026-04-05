package com.reactivecommerce.payment.infrastructure.adapter.persistence;

import com.reactivecommerce.payment.domain.model.Transaction;
import com.reactivecommerce.payment.domain.model.TransactionStatus;
import com.reactivecommerce.payment.domain.port.out.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adaptador MongoDB que implementa TransactionRepository (puerto de salida).
 *
 * Convierte entre el modelo de dominio (Transaction record) y el
 * documento MongoDB (TransactionDocument), aislando el dominio de
 * los detalles de persistencia.
 *
 * Todas las transacciones se almacenan para auditoría regulatoria.
 * No hay TTL — el historial es permanente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionMongoRepository mongoRepository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return mongoRepository.save(toDocument(transaction))
            .map(this::toDomain)
            .doOnSuccess(t -> log.debug("Transaction saved: id={} orderId={} status={}",
                t.id(), t.orderId(), t.status()));
    }

    @Override
    public Mono<Transaction> findByOrderId(UUID orderId) {
        return mongoRepository.findByOrderId(orderId.toString())
            .map(this::toDomain)
            .doOnSuccess(t -> {
                if (t == null) log.debug("Transaction not found for orderId={}", orderId);
            });
    }

    @Override
    public Mono<Transaction> update(Transaction transaction) {
        return mongoRepository.save(toDocument(transaction))
            .map(this::toDomain)
            .doOnSuccess(t -> log.debug("Transaction updated: id={} status={}", t.id(), t.status()));
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private TransactionDocument toDocument(Transaction t) {
        return TransactionDocument.builder()
            .id(t.id().toString())
            .orderId(t.orderId().toString())
            .amount(t.amount())
            .platformCommission(t.platformCommission())
            .creatorAmount(t.creatorAmount())
            .status(t.status().name())
            .gatewayTransactionId(t.gatewayTransactionId())
            .failureCode(t.failureCode())
            .failureMessage(t.failureMessage())
            .createdAt(t.createdAt())
            .build();
    }

    private Transaction toDomain(TransactionDocument d) {
        return Transaction.builder()
            .id(UUID.fromString(d.getId()))
            .orderId(UUID.fromString(d.getOrderId()))
            .amount(d.getAmount())
            .platformCommission(d.getPlatformCommission())
            .creatorAmount(d.getCreatorAmount())
            .status(TransactionStatus.valueOf(d.getStatus()))
            .gatewayTransactionId(d.getGatewayTransactionId())
            .failureCode(d.getFailureCode())
            .failureMessage(d.getFailureMessage())
            .createdAt(d.getCreatedAt())
            .build();
    }
}
