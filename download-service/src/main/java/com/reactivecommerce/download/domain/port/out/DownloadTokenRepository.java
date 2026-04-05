package com.reactivecommerce.download.domain.port.out;

import com.reactivecommerce.download.domain.model.DownloadToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface DownloadTokenRepository {
    Mono<DownloadToken> save(DownloadToken token);
    Mono<DownloadToken> findById(String id);
    Mono<DownloadToken> findByOrderId(UUID orderId);
    /** Todos los tokens del buyer para construir su biblioteca (HU-DWN-02). */
    Flux<DownloadToken> findByBuyerId(UUID buyerId);
    /** Número de regeneraciones ya realizadas para una orden. */
    Mono<Long> countRegenerationsByOrderId(UUID orderId);
    Mono<DownloadToken> update(DownloadToken token);
    Mono<Void> revoke(UUID orderId);
}
