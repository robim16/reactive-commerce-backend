package com.reactivecommerce.review.application.usecase;

import com.reactivecommerce.review.domain.model.Review;
import com.reactivecommerce.review.domain.port.in.ModerateReviewUseCase;
import com.reactivecommerce.review.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.review.domain.port.out.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Implementación del caso de uso de moderación de reseñas.
 *
 * hide():
 *   Cambia el status de la reseña a HIDDEN con un motivo de moderación.
 *   La reseña queda invisible en el catálogo público pero el buyer puede verla.
 *   Publica review.hidden → Notification Service alerta al creator del asset.
 *   La media de ratings del asset NO se recalcula al ocultar (decisión de negocio:
 *   evitar que la moderación altere retroactivamente las estadísticas).
 *
 * restore():
 *   Revierte el estado a VISIBLE.
 *   Idempotente: si ya está VISIBLE retorna sin efecto.
 *   Publica review.restored para que el Notification Service lo notifique.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerateReviewUseCaseImpl implements ModerateReviewUseCase {

    private final ReviewRepository     reviewRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Review> hide(HideCommand command) {
        return reviewRepository.findById(command.reviewId())
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Review not found: " + command.reviewId())))
            .flatMap(review -> {
                if (review.status().name().equals("HIDDEN")) {
                    // Idempotente: ya está oculta
                    return Mono.just(review);
                }
                return reviewRepository.update(review.hide(command.reason()));
            })
            .flatMap(hidden -> eventPublisher.publish(
                    "review.hidden",
                    hidden.assetId().toString(),
                    Map.of(
                        "reviewId", hidden.id(),
                        "assetId",  hidden.assetId(),
                        "buyerId",  hidden.buyerId(),
                        "reason",   command.reason()
                    ))
                .thenReturn(hidden))
            .doOnSuccess(r -> log.info("Review hidden: id={} reason={}", r.id(), command.reason()));
    }

    @Override
    public Mono<Review> restore(String reviewId) {
        return reviewRepository.findById(reviewId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Review not found: " + reviewId)))
            .flatMap(review -> {
                if (review.status().name().equals("VISIBLE")) {
                    // Idempotente: ya está visible
                    return Mono.just(review);
                }
                return reviewRepository.update(review.restore());
            })
            .flatMap(restored -> eventPublisher.publish(
                    "review.restored",
                    restored.assetId().toString(),
                    Map.of(
                        "reviewId", restored.id(),
                        "assetId",  restored.assetId(),
                        "buyerId",  restored.buyerId()
                    ))
                .thenReturn(restored))
            .doOnSuccess(r -> log.info("Review restored: id={}", r.id()));
    }
}
