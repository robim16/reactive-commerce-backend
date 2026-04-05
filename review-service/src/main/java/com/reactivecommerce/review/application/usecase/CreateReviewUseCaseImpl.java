package com.reactivecommerce.review.application.usecase;

import com.reactivecommerce.review.domain.model.Review;
import com.reactivecommerce.review.domain.port.in.CreateReviewUseCase;
import com.reactivecommerce.review.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.review.domain.port.out.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateReviewUseCaseImpl implements CreateReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Review> execute(Command command) {
        if (command.rating() < 1 || command.rating() > 5) {
            return Mono.error(new IllegalArgumentException("Rating must be between 1 and 5"));
        }

        return reviewRepository.findByAssetIdAndBuyerId(command.assetId(), command.buyerId())
            .flatMap(existing -> {
                // Si ya existe, actualizar
                Review updated = existing.withRating(command.rating()).withComment(command.comment());
                return reviewRepository.update(updated);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Review review = Review.create(command.assetId(), command.buyerId(),
                    command.rating(), command.comment());
                return reviewRepository.save(review);
            }))
            .flatMap(saved ->
                reviewRepository.calculateAverageRating(command.assetId())
                    .flatMap(avg -> reviewRepository.countVisibleByAssetId(command.assetId())
                        .flatMap(count -> eventPublisher.publish("review.created",
                            command.assetId().toString(),
                            Map.of("assetId", command.assetId(),
                                   "creatorId", "unknown", // resolver desde product-service
                                   "averageRating", avg,
                                   "totalReviews", count)))
                        .thenReturn(saved))
            );
    }
}
