package com.reactivecommerce.review.application.usecase;

import com.reactivecommerce.review.domain.model.Review;
import com.reactivecommerce.review.domain.port.in.GetReviewsUseCase;
import com.reactivecommerce.review.domain.port.out.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Implementación del caso de uso de consulta de reseñas.
 *
 * findByAsset(assetId):
 *   Solo devuelve reseñas con status VISIBLE.
 *   Invocado desde ReviewHandler en GET /api/v1/reviews/asset/{assetId}.
 *   También lo consume el Product Service vía WebClient para mostrar reseñas
 *   en la página de detalle del asset (SSG con ISR de 2 min en Next.js).
 *
 * findByBuyer(buyerId):
 *   Devuelve todas las reseñas del buyer (VISIBLE e HIDDEN).
 *   El buyer puede ver sus propias reseñas independientemente del estado.
 *   Invocado desde ReviewHandler en GET /api/v1/reviews/buyer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetReviewsUseCaseImpl implements GetReviewsUseCase {

    private final ReviewRepository reviewRepository;

    @Override
    public Flux<Review> findByAsset(UUID assetId) {
        return reviewRepository.findVisibleByAssetId(assetId)
            .doOnComplete(() -> log.debug("Reviews fetched for assetId={}", assetId));
    }

    @Override
    public Flux<Review> findByBuyer(UUID buyerId) {
        return reviewRepository.findByBuyerId(buyerId)
            .doOnComplete(() -> log.debug("Reviews fetched for buyerId={}", buyerId));
    }
}
