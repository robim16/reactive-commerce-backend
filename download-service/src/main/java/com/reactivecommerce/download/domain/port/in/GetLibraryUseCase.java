package com.reactivecommerce.download.domain.port.in;

import com.reactivecommerce.download.domain.model.DownloadToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Consultas de biblioteca: tokens de descarga activos del buyer (HU-DWN-02).
 */
public interface GetLibraryUseCase {
    /** Lista todos los tokens del buyer (biblioteca de assets comprados). */
    Flux<DownloadToken> findByBuyer(UUID buyerId);
    /** Consulta el token activo de una orden específica. */
    Mono<DownloadToken> findByOrder(UUID orderId);
}
