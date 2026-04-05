package com.reactivecommerce.review.domain.port.in;

import com.reactivecommerce.review.domain.model.Review;
import reactor.core.publisher.Mono;

public interface ModerateReviewUseCase {
    record HideCommand(String reviewId, String reason) {}
    Mono<Review> hide(HideCommand command);
    Mono<Review> restore(String reviewId);
}
