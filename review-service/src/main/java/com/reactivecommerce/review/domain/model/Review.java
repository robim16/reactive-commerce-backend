package com.reactivecommerce.review.domain.model;

import lombok.Builder;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
@Document("reviews")
public record Review(
    @Id String id,
    UUID assetId,
    UUID buyerId,
    int rating,
    String comment,
    ReviewStatus status,
    String hideReason,
    Instant createdAt,
    Instant updatedAt
) {
    public static Review create(UUID assetId, UUID buyerId, int rating, String comment) {
        return Review.builder()
            .id(UUID.randomUUID().toString())
            .assetId(assetId).buyerId(buyerId).rating(rating).comment(comment)
            .status(ReviewStatus.VISIBLE)
            .createdAt(Instant.now()).updatedAt(Instant.now())
            .build();
    }

    public Review hide(String reason) {
        return this.withStatus(ReviewStatus.HIDDEN).withHideReason(reason).withUpdatedAt(Instant.now());
    }

    public Review restore() {
        return this.withStatus(ReviewStatus.VISIBLE).withHideReason(null).withUpdatedAt(Instant.now());
    }
}
