package com.reactivecommerce.product.domain.port.in;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface GeneratePresignedUrlUseCase {
    Mono<String> forUpload(UUID assetId, String format);
    Mono<String> forDownload(UUID assetId);
    Mono<String> forModerationPreview(UUID assetId);
}
