package com.reactivecommerce.order.domain.port.out;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

public interface AssetPricePort {
    Mono<BigDecimal> getPrice(UUID assetId);
    Mono<Boolean> isAvailable(UUID assetId);
}
