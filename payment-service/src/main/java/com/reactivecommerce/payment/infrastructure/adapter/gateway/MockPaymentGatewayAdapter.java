package com.reactivecommerce.payment.infrastructure.adapter.gateway;

import com.reactivecommerce.payment.domain.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock del gateway de pagos para desarrollo local y tests.
 * Activo con el perfil "mock-gateway" (default en local).
 *
 * Comportamiento simulado:
 *   - Importes < 0.01          → FAILED (fondos insuficientes)
 *   - Importes >= 9999.99      → FAILED (límite de transacción)
 *   - Cualquier otro importe   → SUCCESS
 *
 * @Primary garantiza que Spring inyecte este bean por encima de
 * PaymentGatewayAdapter cuando el perfil está activo.
 */
@Slf4j
@Primary
@Component
@Profile("mock-gateway")
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public Mono<GatewayResult> charge(UUID buyerId, BigDecimal amount, String idempotencyKey) {
        log.info("[MOCK] Processing charge: buyerId={} amount={} idempotencyKey={}",
            buyerId, amount, idempotencyKey);

        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            return Mono.just(new GatewayResult(null, false, "insufficient_funds"));
        }
        if (amount.compareTo(BigDecimal.valueOf(9999.99)) >= 0) {
            return Mono.just(new GatewayResult(null, false, "transaction_limit_exceeded"));
        }

        String mockTransactionId = "mock_tx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return Mono.just(new GatewayResult(mockTransactionId, true, null));
    }
}
