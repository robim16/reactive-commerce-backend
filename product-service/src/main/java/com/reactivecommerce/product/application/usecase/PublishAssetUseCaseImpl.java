package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.model.AssetStatus;
import com.reactivecommerce.product.domain.port.in.PublishAssetUseCase;
import com.reactivecommerce.product.domain.port.out.AssetCachePort;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import com.reactivecommerce.product.domain.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Implementación del caso de uso de publicación/despublicación de asset.
 *
 * publish():
 *  1. Carga el asset y verifica que el solicitante sea el creador o un admin.
 *  2. Delega al método de dominio Asset.publish() que valida el estado APPROVED.
 *  3. Persiste el cambio de estado a PUBLISHED.
 *  4. Invalida el cache Redis (búsquedas y detalle) para que el catálogo refleje
 *     el nuevo asset de inmediato.
 *  5. Publica asset.published en Kafka → Notification Service actualiza el feed SSE.
 *
 * unpublish():
 *  1. Verifica propiedad del asset.
 *  2. Cambia estado a UNPUBLISHED sin eliminar el asset.
 *     Los compradores que ya lo adquirieron conservan su token de descarga.
 *  3. Invalida cache para que desaparezca del catálogo.
 *  4. NO publica evento Kafka — la despublicación es silenciosa para el feed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishAssetUseCaseImpl implements PublishAssetUseCase {

    private final AssetRepository     assetRepository;
    private final AssetCachePort      assetCachePort;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Asset> publish(UUID assetId, UUID creatorId) {
        return assetRepository.findById(assetId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Asset not found: " + assetId)))
            .flatMap(asset -> {
                if (!asset.creatorId().equals(creatorId)) {
                    return Mono.error(new IllegalStateException(
                        "Only the creator can publish their own asset"));
                }
                // Domain method validates APPROVED status and throws if not
                Asset published;
                try {
                    published = asset.publish();
                } catch (IllegalStateException e) {
                    return Mono.error(e);
                }
                return assetRepository.update(published);
            })
            .flatMap(published ->
                // Invalidate both detail cache and search cache (TTL 5 min in Redis)
                assetCachePort.evict(published.id())
                    .then(assetCachePort.evictSearchCache())
                    .then(eventPublisher.publish(
                        "asset.published",
                        published.id().toString(),
                        Map.of(
                            "assetId",   published.id(),
                            "creatorId", published.creatorId(),
                            "title",     published.title(),
                            "category",  published.category().name()
                        )))
                    .thenReturn(published)
            )
            .doOnSuccess(a -> log.info("Asset published: id={} creatorId={}", a.id(), creatorId));
    }

    @Override
    public Mono<Asset> unpublish(UUID assetId, UUID creatorId) {
        return assetRepository.findById(assetId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Asset not found: " + assetId)))
            .flatMap(asset -> {
                if (!asset.creatorId().equals(creatorId)) {
                    return Mono.error(new IllegalStateException(
                        "Only the creator can unpublish their own asset"));
                }
                if (asset.status() == AssetStatus.UNPUBLISHED) {
                    // Idempotente: ya está despublicado
                    return Mono.just(asset);
                }
                return assetRepository.update(asset.unpublish());
            })
            .flatMap(unpublished ->
                assetCachePort.evict(unpublished.id())
                    .then(assetCachePort.evictSearchCache())
                    .thenReturn(unpublished)
            )
            .doOnSuccess(a -> log.info("Asset unpublished: id={} creatorId={}", a.id(), creatorId));
    }
}
