package com.reactivecommerce.review.domain.port.in;

import com.reactivecommerce.review.domain.model.Review;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface GetReviewsUseCase {
    Flux<Review> findByAsset(UUID assetId);
    Flux<Review> findByBuyer(UUID buyerId);
}
