package com.reactivecommerce.review.domain.port.out;

import com.reactivecommerce.review.domain.model.Review;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ReviewRepository {
    Mono<Review> save(Review review);
    Mono<Review> findById(String id);
    Mono<Review> findByAssetIdAndBuyerId(UUID assetId, UUID buyerId);
    Flux<Review> findVisibleByAssetId(UUID assetId);
    Flux<Review> findByBuyerId(UUID buyerId);
    Mono<Double> calculateAverageRating(UUID assetId);
    Mono<Long> countVisibleByAssetId(UUID assetId);
    Mono<Review> update(Review review);
}
