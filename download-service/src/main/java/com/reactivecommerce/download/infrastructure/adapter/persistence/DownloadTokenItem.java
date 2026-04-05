package com.reactivecommerce.download.infrastructure.adapter.persistence;

import com.reactivecommerce.download.domain.model.DownloadToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Item DynamoDB para DownloadToken.
 *
 * Tabla principal:
 *   Partition key: id
 *
 * GSI 1 — orderId-index:
 *   Partition key: orderId
 *   Usado por: findByOrderId, revoke, countRegenerationsByOrderId
 *
 * GSI 2 — buyerId-index:
 *   Partition key: buyerId
 *   Usado por: findByBuyerId (biblioteca del buyer)
 *
 * TTL attribute: expiresAt (epoch seconds).
 *   DynamoDB elimina el item automáticamente tras la expiración (ventana <= 48h).
 *   DownloadToken.canDownload() verifica Instant.now().isBefore(expiresAt)
 *   como segunda línea de defensa durante esa ventana.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class DownloadTokenItem {

    private String  id;
    private String  orderId;
    private String  buyerId;
    private String  assetId;
    private String  s3Key;
    private int     downloadCount;
    private int     maxDownloads;
    private boolean revoked;
    private long    createdAtEpoch;
    private long    expiresAtEpoch;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    /** GSI 1: orderId-index */
    @DynamoDbSecondaryPartitionKey(indexNames = "orderId-index")
    public String getOrderId() { return orderId; }

    /** GSI 2: buyerId-index */
    @DynamoDbSecondaryPartitionKey(indexNames = "buyerId-index")
    public String getBuyerId() { return buyerId; }

    /** TTL attribute registrado en la configuración de la tabla DynamoDB */
    @DynamoDbAttribute("expiresAt")
    public long getExpiresAtEpoch() { return expiresAtEpoch; }

    // ── Conversiones ──────────────────────────────────────────────────────

    public static DownloadTokenItem from(DownloadToken t) {
        return DownloadTokenItem.builder()
                .id(t.id())
                .orderId(t.orderId().toString())
                .buyerId(t.buyerId().toString())
                .assetId(t.assetId().toString())
                .s3Key(t.s3Key())
                .downloadCount(t.downloadCount())
                .maxDownloads(t.maxDownloads())
                .revoked(t.revoked())
                .createdAtEpoch(t.createdAt().getEpochSecond())
                .expiresAtEpoch(t.expiresAt().getEpochSecond())
                .build();
    }

    public DownloadToken toDomain() {
        return DownloadToken.builder()
                .id(id)
                .orderId(UUID.fromString(orderId))
                .buyerId(UUID.fromString(buyerId))
                .assetId(UUID.fromString(assetId))
                .s3Key(s3Key)
                .downloadCount(downloadCount)
                .maxDownloads(maxDownloads)
                .revoked(revoked)
                .createdAt(Instant.ofEpochSecond(createdAtEpoch))
                .expiresAt(Instant.ofEpochSecond(expiresAtEpoch))
                .build();
    }
}
