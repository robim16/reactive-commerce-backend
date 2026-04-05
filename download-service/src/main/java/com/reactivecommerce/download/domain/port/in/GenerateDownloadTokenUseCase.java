package com.reactivecommerce.download.domain.port.in;

import com.reactivecommerce.download.domain.model.DownloadToken;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface GenerateDownloadTokenUseCase {
    record Command(UUID orderId, UUID buyerId, UUID assetId, String s3Key) {}
    Mono<DownloadToken> execute(Command command);
}
