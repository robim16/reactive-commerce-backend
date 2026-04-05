package com.reactivecommerce.review.infrastructure.adapter.persistence;

import com.reactivecommerce.review.domain.model.Review;
import com.reactivecommerce.review.domain.model.ReviewStatus;
import com.reactivecommerce.review.domain.port.out.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adaptador MongoDB que implementa ReviewRepository (puerto de salida).
 *
 * Convierte entre Review (record de dominio, usa @With de Lombok) y
 * ReviewDocument (POJO de MongoDB). El dominio nunca conoce MongoDB.
 *
 * calculateAverageRating devuelve 0.0 si no hay reseñas visibles
 * (el asset es nuevo o todas las reseñas están ocultas).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private static final String VISIBLE = ReviewStatus.VISIBLE.name();

    private final ReviewMongoRepository mongoRepository;

    @Override
    public Mono<Review> save(Review review) {
        return mongoRepository.save(toDocument(review))
            .map(this::toDomain)
            .doOnSuccess(r -> log.debug("Review saved: id={} assetId={}", r.id(), r.assetId()));
    }

    @Override
    public Mono<Review> findById(String id) {
        return mongoRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Mono<Review> findByAssetIdAndBuyerId(UUID assetId, UUID buyerId) {
        return mongoRepository
            .findByAssetIdAndBuyerId(assetId.toString(), buyerId.toString())
            .map(this::toDomain);
    }

    @Override
    public Flux<Review> findVisibleByAssetId(UUID assetId) {
        return mongoRepository
            .findByAssetIdAndStatusOrderByCreatedAtDesc(assetId.toString(), VISIBLE)
            .map(this::toDomain);
    }

    @Override
    public Flux<Review> findByBuyerId(UUID buyerId) {
        return mongoRepository
            .findByBuyerIdOrderByCreatedAtDesc(buyerId.toString())
            .map(this::toDomain);
    }

    @Override
    public Mono<Double> calculateAverageRating(UUID assetId) {
        return mongoRepository.calculateAverageRating(assetId.toString())
            .map(result -> result.avg() != null ? result.avg() : 0.0)
            .defaultIfEmpty(0.0);
    }

    @Override
    public Mono<Long> countVisibleByAssetId(UUID assetId) {
        return mongoRepository.countByAssetIdAndStatus(assetId.toString(), VISIBLE);
    }

    @Override
    public Mono<Review> update(Review review) {
        return mongoRepository.save(toDocument(review))
            .map(this::toDomain)
            .doOnSuccess(r -> log.debug("Review updated: id={} status={}", r.id(), r.status()));
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private ReviewDocument toDocument(Review r) {
        return ReviewDocument.builder()
            .id(r.id())
            .assetId(r.assetId().toString())
            .buyerId(r.buyerId().toString())
            .rating(r.rating())
            .comment(r.comment())
            .status(r.status().name())
            .hideReason(r.hideReason())
            .createdAt(r.createdAt())
            .updatedAt(r.updatedAt())
            .build();
    }

    private Review toDomain(ReviewDocument d) {
        return Review.builder()
            .id(d.getId())
            .assetId(UUID.fromString(d.getAssetId()))
            .buyerId(UUID.fromString(d.getBuyerId()))
            .rating(d.getRating())
            .comment(d.getComment())
            .status(ReviewStatus.valueOf(d.getStatus()))
            .hideReason(d.getHideReason())
            .createdAt(d.getCreatedAt())
            .updatedAt(d.getUpdatedAt())
            .build();
    }
}
