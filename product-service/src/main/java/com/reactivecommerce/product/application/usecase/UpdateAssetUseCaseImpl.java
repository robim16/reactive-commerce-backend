package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.port.in.UpdateAssetUseCase;
import com.reactivecommerce.product.domain.port.out.AssetCachePort;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del caso de uso de actualización de metadatos (HU-PRO-03).
 *
 * Aplica un merge parcial (PATCH semántico): solo reemplaza los campos
 * no nulos del Command, preservando el resto. Esto permite que el handler
 * pase únicamente los campos que el creator quiso cambiar.
 *
 * Flujo:
 *  1. Carga el asset y verifica propiedad.
 *  2. Aplica los cambios con los métodos @With del record (inmutabilidad).
 *  3. Persiste el asset actualizado.
 *  4. Invalida el cache de detalle y de búsqueda para reflejar
 *     el nuevo precio o descripción en el catálogo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateAssetUseCaseImpl implements UpdateAssetUseCase {

    private final AssetRepository assetRepository;
    private final AssetCachePort  assetCachePort;

    @Override
    public Mono<Asset> execute(Command command) {
        return assetRepository.findById(command.assetId())
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Asset not found: " + command.assetId())))
            .flatMap(asset -> {
                if (!asset.creatorId().equals(command.requesterId())) {
                    return Mono.error(new IllegalStateException(
                        "Only the creator can update their own asset"));
                }
                // Merge parcial: solo aplicar campos presentes en el command
                Asset updated = asset;
                if (command.title()       != null) updated = updated.withTitle(command.title());
                if (command.description() != null) updated = updated.withDescription(command.description());
                if (command.price()       != null) updated = updated.withPrice(command.price());
                if (command.tags()        != null) updated = updated.withTags(command.tags());
                if (command.license()     != null) updated = updated.withLicense(command.license());
                updated = updated.withUpdatedAt(Instant.now());
                return assetRepository.update(updated);
            })
            .flatMap(updated ->
                assetCachePort.evict(updated.id())
                    .then(assetCachePort.evictSearchCache())
                    .thenReturn(updated)
            )
            .doOnSuccess(a -> log.info("Asset updated: id={} requesterId={}",
                a.id(), command.requesterId()));
    }
}
