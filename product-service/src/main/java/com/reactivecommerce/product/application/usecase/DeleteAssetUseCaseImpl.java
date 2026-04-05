package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.port.in.DeleteAssetUseCase;
import com.reactivecommerce.product.domain.port.out.AssetCachePort;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import com.reactivecommerce.product.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementación del caso de uso de eliminación de asset (HU-PRO-03).
 *
 * Flujo:
 *  1. Carga el asset y verifica que el solicitante sea el creador.
 *  2. Verifica que no tenga ventas — si tiene, rechaza con mensaje claro.
 *  3. Elimina el archivo original de S3 (usando StoragePort.deleteObject).
 *  4. Elimina la miniatura de S3 si existe (thumbnailS3Key != null).
 *  5. Elimina el registro de la base de datos.
 *  6. Invalida el cache para que desaparezca del catálogo y del detalle.
 *
 * El paso 3-4 es fire-and-forget respecto a S3: si S3 falla, el registro
 * en base de datos igualmente se borra. El archivo huérfano en S3 se limpia
 * mediante una política de ciclo de vida de S3 (lifecycle rule).
 * Esta decisión evita dejar el sistema en un estado inconsistente donde
 * el asset existe en DB pero no en S3 (que sería peor).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteAssetUseCaseImpl implements DeleteAssetUseCase {

    private final AssetRepository assetRepository;
    private final AssetCachePort  assetCachePort;
    private final StoragePort     storagePort;

    @Override
    public Mono<Void> execute(Command command) {
        return assetRepository.findById(command.assetId())
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Asset not found: " + command.assetId())))
            .flatMap(asset -> {
                // Verificar propiedad
                if (!asset.creatorId().equals(command.requesterId())) {
                    return Mono.error(new IllegalStateException(
                        "Only the creator can delete their own asset"));
                }
                // Verificar que no tenga ventas (HU-PRO-03 criterio 4)
                if (asset.totalSales() != null && asset.totalSales() > 0) {
                    return Mono.error(new IllegalStateException(
                        "Cannot delete an asset with sales. Use unpublish instead."));
                }
                // Eliminar archivos S3 (fire-and-forget individual, no bloquea la eliminación en DB)
                Mono<Void> deleteOriginal = asset.s3Key() != null
                    ? storagePort.deleteObject(asset.s3Key())
                        .onErrorResume(e -> {
                            log.warn("Failed to delete S3 object {}: {}", asset.s3Key(), e.getMessage());
                            return Mono.empty();
                        })
                    : Mono.empty();

                Mono<Void> deleteThumbnail = asset.thumbnailS3Key() != null
                    ? storagePort.deleteObject(asset.thumbnailS3Key())
                        .onErrorResume(e -> {
                            log.warn("Failed to delete S3 thumbnail {}: {}", asset.thumbnailS3Key(), e.getMessage());
                            return Mono.empty();
                        })
                    : Mono.empty();

                // Borrar S3 en paralelo, luego borrar en DB
                return deleteOriginal
                    .then(deleteThumbnail)
                    .then(assetRepository.deleteById(command.assetId()));
            })
            .then(assetCachePort.evict(command.assetId())
                .then(assetCachePort.evictSearchCache()))
            .doOnSuccess(v -> log.info("Asset deleted: id={} requesterId={}",
                command.assetId(), command.requesterId()));
    }
}
