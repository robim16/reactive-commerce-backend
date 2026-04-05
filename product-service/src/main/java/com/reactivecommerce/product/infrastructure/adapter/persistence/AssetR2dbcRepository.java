package com.reactivecommerce.product.infrastructure.adapter.persistence;

import com.reactivecommerce.product.domain.model.AssetStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface AssetR2dbcRepository extends ReactiveCrudRepository<AssetEntity, UUID> {
    Flux<AssetEntity> findByStatusOrderByCreatedAtDesc(AssetStatus status);
    Flux<AssetEntity> findByCategoryAndStatusOrderByCreatedAtDesc(
        com.reactivecommerce.product.domain.model.AssetCategory category, AssetStatus status);
}
