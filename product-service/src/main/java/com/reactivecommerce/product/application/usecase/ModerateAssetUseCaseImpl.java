package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.port.in.ModerateAssetUseCase;
import com.reactivecommerce.product.domain.port.out.AssetCachePort;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import com.reactivecommerce.product.domain.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModerateAssetUseCaseImpl implements ModerateAssetUseCase {

    private final AssetRepository assetRepository;
    private final AssetCachePort assetCachePort;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Asset> approve(ApproveCommand command) {
        return assetRepository.findById(command.assetId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Asset not found")))
            .map(asset -> asset.approve(command.moderatorId()))
            .flatMap(assetRepository::update)
            .flatMap(updated -> assetCachePort.evict(updated.id())
                .then(eventPublisher.publish("asset.approved", updated.id().toString(),
                    Map.of("assetId", updated.id(), "creatorId", updated.creatorId())))
                .thenReturn(updated));
    }

    @Override
    public Mono<Asset> reject(RejectCommand command) {
        return assetRepository.findById(command.assetId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Asset not found")))
            .map(asset -> asset.reject(command.reason(), command.moderatorId()))
            .flatMap(assetRepository::update)
            .flatMap(updated -> assetCachePort.evict(updated.id())
                .then(eventPublisher.publish("asset.rejected", updated.id().toString(),
                    Map.of("assetId", updated.id(), "creatorId", updated.creatorId(),
                           "reason", command.reason())))
                .thenReturn(updated));
    }
}
