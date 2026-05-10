package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.port.in.SearchAssetsUseCase;
import com.reactivecommerce.product.domain.port.out.AssetCachePort;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchAssetsUseCaseImpl implements SearchAssetsUseCase {

    private final AssetRepository assetRepository;
    private final AssetCachePort assetCachePort;

    @Override
    public Flux<Asset> search(Query query) {
        return assetRepository.search(query);
    }

    @Override
    public Mono<Asset> findById(UUID id) {
        return assetCachePort.getOrLoad(id, assetRepository.findById(id));
    }

    @Override
    public Flux<Asset> searchAll(Query query) { return assetRepository.searchAll(query); }
}
