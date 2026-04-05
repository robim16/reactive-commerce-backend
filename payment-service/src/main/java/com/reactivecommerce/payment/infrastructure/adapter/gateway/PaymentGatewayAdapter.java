package com.reactivecommerce.payment.infrastructure.adapter.gateway;

import com.reactivecommerce.payment.domain.port.out.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adaptador del Payment Gateway externo.
 *
 * Implementa PaymentGatewayPort encapsulando la comunicación con el
 * gateway de pagos (Stripe, PayU, MercadoPago, etc.). El dominio nunca
 * conoce qué gateway se usa — solo recibe un GatewayResult.
 *
 * Clasificación de errores:
 *
 *   Transitorios (se reintenta con backoff — configurado en Resilience4j):
 *     - Timeout / IOException → red inestable
 *     - 503 Service Unavailable → gateway momentáneamente caído
 *
 *   Definitivos (NO se reintenta — Resilience4j ignoreExceptions):
 *     - 402 Payment Required → fondos insuficientes
 *     - 422 Unprocessable    → tarjeta inválida / rechazada
 *     - 400 Bad Request      → datos malformados
 *
 * El idempotencyKey (UUID de la transacción) garantiza que si el gateway
 * recibe la misma solicitud dos veces (reintento), no cobra dos veces.
 *
 * En local/test se activa el perfil "mock-gateway" que usa
 * MockPaymentGatewayAdapter en lugar de este adaptador real.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayAdapter implements PaymentGatewayPort {

    private final WebClient gatewayWebClient;

    @Value("${payment.gateway.api-key:test-key}")
    private String apiKey;

    /**
     * Respuesta del gateway externo (estructura simplificada).
     * En producción mapear al DTO real del proveedor elegido.
     */
    private record GatewayChargeRequest(
        String buyerId,
        String amount,
        String currency,
        String idempotencyKey
    ) {}

    private record GatewayChargeResponse(
        String id,
        String status,        // "succeeded" | "failed"
        String failureCode    // "card_declined" | "insufficient_funds" | etc.
    ) {}

    @Override
    public Mono<GatewayResult> charge(UUID buyerId, BigDecimal amount, String idempotencyKey) {
        GatewayChargeRequest body = new GatewayChargeRequest(
            buyerId.toString(),
            amount.toPlainString(),
            "USD",
            idempotencyKey
        );

        return gatewayWebClient.post()
            .uri("/v1/charges")
            .header("Authorization", "Bearer " + apiKey)
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(GatewayChargeResponse.class)
            .map(resp -> new GatewayResult(
                resp.id(),
                "succeeded".equals(resp.status()),
                resp.failureCode()
            ))
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.warn("Gateway HTTP error: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
                // 4xx definitivos → marcar como fallido sin propagar excepción
                if (ex.getStatusCode().is4xxClientError()) {
                    return Mono.just(new GatewayResult(
                        null, false, "gateway_" + ex.getStatusCode().value()
                    ));
                }
                // 5xx → propagar para que Resilience4j lo reintente
                return Mono.error(ex);
            })
            .doOnSuccess(r -> log.info("Gateway charge: idempotencyKey={} success={} failureCode={}",
                idempotencyKey, r.success(), r.failureCode()));
    }
}
