package com.reactivecommerce.product.domain.port.in;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.model.AssetCategory;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CreateAssetUseCase {
    record Command(String title, String description, AssetCategory category,
                   List<String> tags, BigDecimal price, String license,
                   UUID creatorId, String format, Long fileSizeBytes) {}
    Mono<AssetUploadSession> execute(Command command);

    record AssetUploadSession(UUID assetId, String presignedUploadUrl) {}
}
