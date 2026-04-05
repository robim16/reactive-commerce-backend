package com.reactivecommerce.payment.application.usecase;

import com.reactivecommerce.payment.domain.model.Transaction;
import com.reactivecommerce.payment.domain.model.TransactionStatus;
import com.reactivecommerce.payment.domain.port.in.ProcessPaymentUseCase;
import com.reactivecommerce.payment.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.payment.domain.port.out.PaymentGatewayPort;
import com.reactivecommerce.payment.domain.port.out.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final TransactionRepository transactionRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final DomainEventPublisher eventPublisher;

    @Value("${payment.commission-rate:0.20}")
    private BigDecimal commissionRate;

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentGateway")
    public Mono<Transaction> execute(Command command) {
        Transaction tx = Transaction.create(command.orderId(), command.amount(), commissionRate);
        return transactionRepository.save(tx)
            .flatMap(saved -> paymentGatewayPort
                .charge(command.buyerId(), command.amount(), saved.id().toString())
                .flatMap(result -> {
                    if (result.success()) {
                        Transaction completed = Transaction.builder()
                            .id(saved.id())
                            .orderId(saved.orderId())
                            .amount(saved.amount())
                            .platformCommission(saved.platformCommission())
                            .creatorAmount(saved.creatorAmount())
                            .status(TransactionStatus.COMPLETED)
                            .gatewayTransactionId(result.transactionId())
                            .failureCode(saved.failureCode())
                            .failureMessage(saved.failureMessage())
                            .createdAt(saved.createdAt())
                            .build();
                        return transactionRepository.update(completed)
                            .flatMap(updated -> eventPublisher.publish(
                                "order.payment_completed", command.orderId().toString(),
                                Map.of("orderId", command.orderId(),
                                       "transactionId", result.transactionId(),
                                       "amount", command.amount()))
                                .thenReturn(updated));
                    } else {
                        Transaction failed = Transaction.builder()
                            .id(saved.id())
                            .orderId(saved.orderId())
                            .amount(saved.amount())
                            .platformCommission(saved.platformCommission())
                            .creatorAmount(saved.creatorAmount())
                            .status(TransactionStatus.FAILED)
                            .gatewayTransactionId(saved.gatewayTransactionId())
                            .failureCode(result.failureCode())
                            .failureMessage(saved.failureMessage())
                            .createdAt(saved.createdAt())
                            .build();
                        return transactionRepository.update(failed)
                            .flatMap(updated -> eventPublisher.publish(
                                "order.payment_failed", command.orderId().toString(),
                                Map.of("orderId", command.orderId(),
                                       "reason", result.failureCode()))
                                .thenReturn(updated));
                    }
                })
            );
    }

    public Mono<Transaction> paymentFallback(Command command, Throwable t) {
        log.error("Payment gateway circuit open for order {}: {}", command.orderId(), t.getMessage());
        return eventPublisher.publish("order.payment_failed", command.orderId().toString(),
            Map.of("orderId", command.orderId(), "reason", "Payment service temporarily unavailable"))
            .then(Mono.error(new RuntimeException("Payment gateway unavailable")));
    }
}
