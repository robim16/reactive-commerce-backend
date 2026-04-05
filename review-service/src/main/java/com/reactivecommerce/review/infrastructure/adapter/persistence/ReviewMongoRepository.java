package com.reactivecommerce.review.infrastructure.adapter.persistence;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data MongoDB Reactive.
 *
 * calculateAverageRating usa un pipeline de agregación MongoDB:
 *   { $match: { assetId, status: "VISIBLE" } },
 *   { $group: { _id: null, avg: { $avg: "$rating" } } }
 * Esto es más eficiente que cargar todos los documentos en memoria y calcular en Java.
 */
public interface ReviewMongoRepository
        extends ReactiveMongoRepository<ReviewDocument, String> {

    Flux<ReviewDocument> findByAssetIdAndStatusOrderByCreatedAtDesc(String assetId, String status);

    Flux<ReviewDocument> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    Mono<ReviewDocument> findByAssetIdAndBuyerId(String assetId, String buyerId);

    Mono<Long> countByAssetIdAndStatus(String assetId, String status);

    @Aggregation(pipeline = {
        "{ $match: { assetId: ?0, status: 'VISIBLE' } }",
        "{ $group: { _id: null, avg: { $avg: '$rating' } } }"
    })
    Mono<AverageResult> calculateAverageRating(String assetId);

    record AverageResult(Double avg) {}
}
