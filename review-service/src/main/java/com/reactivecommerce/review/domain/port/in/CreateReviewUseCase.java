package com.reactivecommerce.review.domain.port.in;

import com.reactivecommerce.review.domain.model.Review;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface CreateReviewUseCase {
    record Command(UUID assetId, UUID buyerId, int rating, String comment) {}
    Mono<Review> execute(Command command);
}
