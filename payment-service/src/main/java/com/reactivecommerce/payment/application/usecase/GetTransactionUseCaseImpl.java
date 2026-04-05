package com.reactivecommerce.payment.application.usecase;

import com.reactivecommerce.payment.domain.model.Transaction;
import com.reactivecommerce.payment.domain.port.in.GetTransactionUseCase;
import com.reactivecommerce.payment.domain.port.out.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTransactionUseCaseImpl implements GetTransactionUseCase {

    private final TransactionRepository transactionRepository;

    @Override
    public Mono<Transaction> findByOrderId(UUID orderId) {
        return transactionRepository.findByOrderId(orderId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Transaction not found for orderId=" + orderId)));
    }
}
