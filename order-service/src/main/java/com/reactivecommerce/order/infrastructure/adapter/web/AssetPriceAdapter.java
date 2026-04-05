package com.reactivecommerce.order.infrastructure.adapter.web;

import com.reactivecommerce.order.domain.port.out.AssetPricePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adaptador WebClient que implementa AssetPricePort.
 *
 * Consulta el Product Service (via API Gateway o Eureka lb://) para:
 *  - getPrice()      → GET /api/v1/assets/{id}  → extrae el campo price
 *  - isAvailable()   → GET /api/v1/assets/{id}  → verifica status == PUBLISHED
 *
 * Aplica Circuit Breaker + Retry (Resilience4j) en cada llamada para
 * proteger al Order Service si el Product Service no responde.
 * El fallback de isAvailable() devuelve false (denegar la compra ante duda).
 * El fallback de getPrice() propaga el error para no crear pedidos con precio 0.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetPriceAdapter implements AssetPricePort {

    private final WebClient productServiceClient;

    // ── Respuesta mínima del Product Service ─────────────────────────────
    private record AssetResponse(String id, BigDecimal price, String status) {}

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "getPriceFallback")
    @Retry(name = "productService")
    public Mono<BigDecimal> getPrice(UUID assetId) {
        return productServiceClient.get()
            .uri("/api/v1/assets/{id}", assetId)
            .retrieve()
            .bodyToMono(AssetResponse.class)
            .map(AssetResponse::price)
            .doOnSuccess(p -> log.debug("Price fetched for assetId={}: {}", assetId, p));
    }

    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "isAvailableFallback")
    @Retry(name = "productService")
    public Mono<Boolean> isAvailable(UUID assetId) {
        return productServiceClient.get()
            .uri("/api/v1/assets/{id}", assetId)
            .retrieve()
            .bodyToMono(AssetResponse.class)
            .map(asset -> "PUBLISHED".equals(asset.status()))
            .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                log.warn("Asset not found: {}", assetId);
                return Mono.just(false);
            })
            .doOnSuccess(a -> log.debug("Availability for assetId={}: {}", assetId, a));
    }

    // ── Fallbacks de Circuit Breaker ──────────────────────────────────────

    public Mono<BigDecimal> getPriceFallback(UUID assetId, Throwable t) {
        log.error("Circuit open for getPrice assetId={}: {}", assetId, t.getMessage());
        return Mono.error(new RuntimeException(
            "Product Service unavailable — cannot retrieve price for asset " + assetId));
    }

    public Mono<Boolean> isAvailableFallback(UUID assetId, Throwable t) {
        log.error("Circuit open for isAvailable assetId={}: {}", assetId, t.getMessage());
        // Fail-safe: deny purchase when product service is down
        return Mono.just(false);
    }
}
