package com.reactivecommerce.download.infrastructure.adapter.persistence;

import com.reactivecommerce.download.domain.model.DownloadToken;
import com.reactivecommerce.download.domain.port.out.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.UUID;

/**
 * Adaptador DynamoDB para tokens de descarga.
 *
 * Tabla:          download-tokens
 * Partition key:  id  (UUID del token, embebido en el link de descarga)
 * GSI 1:          orderId-index    → pk: orderId
 *                 Usado para: findByOrderId, revoke, countRegenerations
 * GSI 2:          buyerId-index    → pk: buyerId
 *                 Usado para: findByBuyerId (biblioteca del buyer)
 * TTL attribute:  expiresAt (epoch seconds — DynamoDB limpia automáticamente)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadTokenRepositoryAdapter implements DownloadTokenRepository {

    private final DynamoDbEnhancedAsyncClient dynamoDbClient;
    private final DynamoDbAsyncTable<DownloadTokenItem> table;

    @Override
    public Mono<DownloadToken> save(DownloadToken token) {
        DownloadTokenItem item = DownloadTokenItem.from(token);
        return Mono.fromFuture(table.putItem(item))
                .thenReturn(token)
                .doOnSuccess(t -> log.debug("Token saved: id={} orderId={}", t.id(), t.orderId()));
    }

    @Override
    public Mono<DownloadToken> update(DownloadToken token) {
        DownloadTokenItem item = DownloadTokenItem.from(token);
        return Mono.fromFuture(table.updateItem(item))
                .map(DownloadTokenItem::toDomain)
                .doOnSuccess(t -> log.debug("Token updated: id={} downloads={}/{} revoked={}",
                        t.id(), t.downloadCount(), t.maxDownloads(), t.revoked()));
    }

    @Override
    public Mono<Void> revoke(UUID orderId) {
        return findByOrderId(orderId)
                .flatMap(token -> {
                    DownloadTokenItem revokedItem = DownloadTokenItem.from(token.withRevoked(true));
                    return Mono.fromFuture(table.updateItem(revokedItem));
                })
                .doOnSuccess(v -> log.info("Token revoked for orderId={}", orderId))
                .then();
    }


    @Override
    public Mono<DownloadToken> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Mono.fromFuture(table.getItem(key))
                .map(DownloadTokenItem::toDomain)
                .doOnSuccess(t -> {
                    if (t == null) log.debug("Token not found: id={}", id);
                });
    }

    @Override
    public Mono<DownloadToken> findByOrderId(UUID orderId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(orderId.toString()).build());

        return Mono.from(
                        table.index("orderId-index")
                                .query(condition)
                                .flatMapIterable(page -> page.items())
                )
                .map(DownloadTokenItem::toDomain)
                .doOnSuccess(t -> {
                    if (t == null) log.debug("Token not found for orderId={}", orderId);
                });
    }

    /**
     * Lista todos los tokens del buyer para construir la biblioteca (HU-DWN-02).
     * Consulta el GSI buyerId-index sin necesidad de escaneo completo.
     * Incluye tokens activos, expirados y revocados — el handler/frontend filtra
     * visualmente, pero todos son relevantes para el historial.
     */
    @Override
    public Flux<DownloadToken> findByBuyerId(UUID buyerId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(buyerId.toString()).build());

        return Flux.from(
                        table.index("buyerId-index")
                                .query(condition)
                                .flatMapIterable(page -> page.items())
                )
                .map(DownloadTokenItem::toDomain)
                .doOnComplete(() -> log.debug("Library loaded for buyerId={}", buyerId));
    }

    /**
     * Cuenta las regeneraciones de una orden contando los tokens con el mismo
     * orderId. El primero es el original; cada token adicional es una regeneración.
     * Si count=0 (no hay token) o count=1 (solo el original), regenerations=0.
     */
    @Override
    public Mono<Long> countRegenerationsByOrderId(UUID orderId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(orderId.toString()).build());

        return Flux.from(
                        table.index("orderId-index")
                                .query(condition)
                                .flatMapIterable(page -> page.items())
                )
                .count()
                // El primer token es el original, los adicionales son regeneraciones
                .map(total -> Math.max(0L, total - 1L));
    }
}
