package com.reactivecommerce.product.infrastructure.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.port.out.AssetCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetRedisCacheAdapter implements AssetCachePort {

    private static final String ASSET_KEY = "asset:";
    private static final String SEARCH_KEY = "search:*";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> evict(UUID assetId) {
        return redisTemplate.delete(ASSET_KEY + assetId).then();
    }

    @Override
    public Mono<Void> evictSearchCache() {
        return redisTemplate.keys(SEARCH_KEY)
            .flatMap(redisTemplate::delete)
            .then();
    }

    @Override
    public Mono<Asset> getOrLoad(UUID assetId, Mono<Asset> loader) {
        String key = ASSET_KEY + assetId;
        return redisTemplate.opsForValue().get(key)
            .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, Asset.class)))
            .switchIfEmpty(loader.flatMap(asset ->
                Mono.fromCallable(() -> objectMapper.writeValueAsString(asset))
                    .flatMap(json -> redisTemplate.opsForValue().set(key, json, TTL))
                    .thenReturn(asset)
            ))
            .onErrorResume(e -> {
                log.warn("Cache error, falling back to DB: {}", e.getMessage());
                return loader;
            });
    }
}
