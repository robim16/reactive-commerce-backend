package com.reactivecommerce.download.domain.port.in;

import com.reactivecommerce.download.domain.model.DownloadToken;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Regenera un token de descarga expirado para una orden ya pagada (HU-DWN-02).
 * Máximo 3 regeneraciones por asset, configurable en Config Service.
 */
public interface RegenerateTokenUseCase {
    record Command(UUID orderId, UUID buyerId) {}
    Mono<DownloadToken> execute(Command command);
}
