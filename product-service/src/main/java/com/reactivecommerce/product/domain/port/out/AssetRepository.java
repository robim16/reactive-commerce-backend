package com.reactivecommerce.product.domain.port.out;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.model.AssetStatus;
import com.reactivecommerce.product.domain.port.in.SearchAssetsUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface AssetRepository {
    Mono<Asset> save(Asset asset);
    Mono<Asset> findById(UUID id);
    Flux<Asset> search(SearchAssetsUseCase.Query query);
    Flux<Asset> findByStatus(AssetStatus status);
    Mono<Asset> update(Asset asset);
    /** Elimina el asset por ID. No verifica reglas de negocio — eso es responsabilidad del use case. */
    Mono<Void> deleteById(UUID id);
}
