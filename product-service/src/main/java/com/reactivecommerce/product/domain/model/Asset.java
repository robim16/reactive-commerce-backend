package com.reactivecommerce.product.domain.model;

import lombok.Builder;
import lombok.With;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@With
public record Asset(
    UUID id,
    String title,
    String description,
    AssetCategory category,
    List<String> tags,
    BigDecimal price,
    String license,
    AssetStatus status,
    UUID creatorId,
    String s3Key,
    String thumbnailS3Key,
    String format,
    Long fileSizeBytes,
    Double averageRating,
    Integer totalReviews,
    Integer totalSales,
    String rejectionReason,
    UUID moderatedBy,
    Instant moderatedAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static Asset create(String title, String description, AssetCategory category,
                                List<String> tags, BigDecimal price, String license,
                                UUID creatorId, String format) {
        return Asset.builder()
            .id(UUID.randomUUID())
            .title(title).description(description).category(category)
            .tags(tags).price(price).license(license)
            .status(AssetStatus.PENDING_MODERATION)
            .creatorId(creatorId).format(format)
            .averageRating(0.0).totalReviews(0).totalSales(0)
            .createdAt(Instant.now()).updatedAt(Instant.now())
            .build();
    }

    public Asset approve(UUID moderatorId) {
        return this.withStatus(AssetStatus.APPROVED)
            .withModeratedBy(moderatorId)
            .withModeratedAt(Instant.now())
            .withUpdatedAt(Instant.now());
    }

    public Asset reject(String reason, UUID moderatorId) {
        return this.withStatus(AssetStatus.REJECTED)
            .withRejectionReason(reason)
            .withModeratedBy(moderatorId)
            .withModeratedAt(Instant.now())
            .withUpdatedAt(Instant.now());
    }

    public Asset publish() {
        if (this.status != AssetStatus.APPROVED) {
            throw new IllegalStateException("Only approved assets can be published");
        }
        return this.withStatus(AssetStatus.PUBLISHED).withUpdatedAt(Instant.now());
    }

    public Asset unpublish() {
        return this.withStatus(AssetStatus.UNPUBLISHED).withUpdatedAt(Instant.now());
    }
}
