package com.reactivecommerce.product.domain.port.in;

import com.reactivecommerce.product.domain.model.Asset;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ModerateAssetUseCase {
    record ApproveCommand(UUID assetId, UUID moderatorId) {}
    record RejectCommand(UUID assetId, UUID moderatorId, String reason) {}
    Mono<Asset> approve(ApproveCommand command);
    Mono<Asset> reject(RejectCommand command);
}
