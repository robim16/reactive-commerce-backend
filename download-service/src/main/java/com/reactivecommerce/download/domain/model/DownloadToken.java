package com.reactivecommerce.download.domain.model;

import lombok.Builder;
import lombok.With;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
public record DownloadToken(
    String id,           // UUID string — clave primaria en DynamoDB
    UUID orderId,
    UUID buyerId,
    UUID assetId,
    String s3Key,
    int downloadCount,
    int maxDownloads,
    boolean revoked,
    Instant createdAt,
    Instant expiresAt    // TTL DynamoDB
) {
    public static DownloadToken create(UUID orderId, UUID buyerId, UUID assetId,
                                       String s3Key, int maxDownloads) {
        return DownloadToken.builder()
            .id(UUID.randomUUID().toString())
            .orderId(orderId).buyerId(buyerId).assetId(assetId).s3Key(s3Key)
            .downloadCount(0).maxDownloads(maxDownloads).revoked(false)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(86400)) // 24h TTL
            .build();
    }

    public boolean canDownload() {
        return !revoked && downloadCount < maxDownloads && Instant.now().isBefore(expiresAt);
    }

    public DownloadToken incrementCount() {
        return this.withDownloadCount(this.downloadCount + 1);
    }
}
