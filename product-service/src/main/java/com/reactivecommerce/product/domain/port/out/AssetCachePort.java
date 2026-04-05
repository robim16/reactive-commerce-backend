package com.reactivecommerce.product.domain.port.out;

import com.reactivecommerce.product.domain.model.Asset;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface AssetCachePort {
    Mono<Void> evict(UUID assetId);
    Mono<Void> evictSearchCache();
    Mono<Asset> getOrLoad(UUID assetId, Mono<Asset> loader);
}
