package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.port.in.CreateAssetUseCase;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import com.reactivecommerce.product.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.product.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAssetUseCaseImpl implements CreateAssetUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<AssetUploadSession> execute(Command command) {
        Asset asset = Asset.create(command.title(), command.description(), command.category(),
            command.tags(), command.price(), command.license(),
            command.creatorId(), command.format());

        String s3Key = "assets/" + asset.id() + "/original." + command.format().toLowerCase();

        return assetRepository.save(asset.withS3Key(s3Key).withFileSizeBytes(command.fileSizeBytes()))
            .flatMap(saved ->
                storagePort.generateUploadPresignedUrl(s3Key, Duration.ofMinutes(30))
                    .map(url -> new AssetUploadSession(saved.id(), url))
            )
            .flatMap(session -> eventPublisher
                .publish("asset.uploaded", asset.id().toString(),
                    Map.of("assetId", asset.id(), "creatorId", command.creatorId(),
                           "title", command.title()))
                .thenReturn(session))
            .doOnSuccess(s -> log.info("Asset created: {}", s.assetId()));
    }
}
