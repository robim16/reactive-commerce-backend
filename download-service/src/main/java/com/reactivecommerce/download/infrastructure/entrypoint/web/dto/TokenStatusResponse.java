package com.reactivecommerce.download.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.download.domain.model.DownloadToken;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de estado de un token de descarga.
 * Expone exactamente lo que el frontend necesita para renderizar
 * el botón de descarga en la biblioteca y el historial de pedidos.
 */
public record TokenStatusResponse(
        String  tokenId,
        UUID    orderId,
        UUID    assetId,
        int     downloadCount,
        int     maxDownloads,
        int     downloadsRemaining,
        boolean revoked,
        boolean expired,
        boolean downloadable,
        Instant expiresAt,
        Instant createdAt
) {
    public static TokenStatusResponse from(DownloadToken t) {
        boolean expired = Instant.now().isAfter(t.expiresAt());
        return new TokenStatusResponse(
                t.id(),
                t.orderId(),
                t.assetId(),
                t.downloadCount(),
                t.maxDownloads(),
                Math.max(0, t.maxDownloads() - t.downloadCount()),
                t.revoked(),
                expired,
                t.canDownload(),
                t.expiresAt(),
                t.createdAt()
        );
    }
}
