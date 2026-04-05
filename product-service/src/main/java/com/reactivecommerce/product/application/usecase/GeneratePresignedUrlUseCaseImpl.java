package com.reactivecommerce.product.application.usecase;

import com.reactivecommerce.product.domain.port.in.GeneratePresignedUrlUseCase;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import com.reactivecommerce.product.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Implementación del caso de uso de generación de presigned URLs de S3.
 *
 * Tres operaciones diferenciadas:
 *
 *  forUpload(assetId, format):
 *    Genera una PUT presigned URL válida 30 minutos para que el creator
 *    suba el archivo directamente a S3 sin pasar por el backend.
 *    La s3Key sigue el patrón: assets/{assetId}/original.{format}
 *    Se usa durante el flujo de creación de asset (HU-PRO-01).
 *
 *  forDownload(assetId):
 *    Genera una GET presigned URL válida 15 minutos para descarga directa.
 *    Solo para assets PUBLISHED. Usado por el ProductHandler en la ruta
 *    GET /api/v1/assets/{id}/download-url.
 *    Nota: la descarga real de assets comprados la gestiona el Download Service
 *    con tokens DynamoDB. Esta URL es para previsualización pública temporal.
 *
 *  forModerationPreview(assetId):
 *    Genera una GET presigned URL válida 60 minutos para que el moderador
 *    descargue y revise el asset completo antes de aprobar/rechazar.
 *    TTL más largo porque la revisión puede tardar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePresignedUrlUseCaseImpl implements GeneratePresignedUrlUseCase {

    private static final Duration UPLOAD_TTL            = Duration.ofMinutes(30);
    private static final Duration DOWNLOAD_TTL          = Duration.ofMinutes(15);
    private static final Duration MODERATION_PREVIEW_TTL = Duration.ofMinutes(60);

    private final AssetRepository assetRepository;
    private final StoragePort     storagePort;

    @Override
    public Mono<String> forUpload(UUID assetId, String format) {
        // La s3Key para upload se construye localmente; no necesitamos cargar el asset
        // porque el asset ya fue creado en CreateAssetUseCaseImpl con esa misma clave
        String s3Key = buildOriginalKey(assetId, format);
        return storagePort.generateUploadPresignedUrl(s3Key, UPLOAD_TTL)
            .doOnSuccess(url -> log.debug(
                "Upload presigned URL generated: assetId={} s3Key={}", assetId, s3Key));
    }

    @Override
    public Mono<String> forDownload(UUID assetId) {
        return assetRepository.findById(assetId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Asset not found: " + assetId)))
            .flatMap(asset -> {
                if (asset.s3Key() == null) {
                    return Mono.error(new IllegalStateException(
                        "Asset has no file uploaded yet: " + assetId));
                }
                return storagePort.generateDownloadPresignedUrl(asset.s3Key(), DOWNLOAD_TTL);
            })
            .doOnSuccess(url -> log.debug(
                "Download presigned URL generated: assetId={}", assetId));
    }

    @Override
    public Mono<String> forModerationPreview(UUID assetId) {
        return assetRepository.findById(assetId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Asset not found: " + assetId)))
            .flatMap(asset -> {
                if (asset.s3Key() == null) {
                    return Mono.error(new IllegalStateException(
                        "Asset has no file uploaded yet: " + assetId));
                }
                return storagePort.generateDownloadPresignedUrl(asset.s3Key(), MODERATION_PREVIEW_TTL);
            })
            .doOnSuccess(url -> log.debug(
                "Moderation preview URL generated: assetId={}", assetId));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Construye la s3Key canónica del archivo original del asset.
     * Debe coincidir con la clave usada en CreateAssetUseCaseImpl.
     */
    private String buildOriginalKey(UUID assetId, String format) {
        return "assets/" + assetId + "/original." + format.toLowerCase();
    }
}
