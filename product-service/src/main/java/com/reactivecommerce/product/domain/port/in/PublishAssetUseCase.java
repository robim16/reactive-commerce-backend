package com.reactivecommerce.product.domain.port.in;

import com.reactivecommerce.product.domain.model.Asset;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface PublishAssetUseCase {
    Mono<Asset> publish(UUID assetId, UUID creatorId);
    Mono<Asset> unpublish(UUID assetId, UUID creatorId);
}
