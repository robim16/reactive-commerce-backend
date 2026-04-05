package com.reactivecommerce.review.infrastructure.entrypoint.web;

import com.reactivecommerce.review.domain.model.Review;
import com.reactivecommerce.review.domain.port.in.*;
import com.reactivecommerce.review.infrastructure.entrypoint.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler reactivo del Review Service.
 */
@Component
@RequiredArgsConstructor
public class ReviewHandler {

    private final CreateReviewUseCase createReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        UUID buyerId = extractUserId(request);
        return request.bodyToMono(CreateReviewRequest.class)
            .flatMap(body -> createReviewUseCase.execute(
                new CreateReviewUseCase.Command(
                    UUID.fromString(body.assetId()), buyerId,
                    body.rating(), body.comment())))
            .flatMap(r -> ServerResponse.status(HttpStatus.CREATED).bodyValue(r))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> findByAsset(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("assetId"));
        return ServerResponse.ok()
            .body(getReviewsUseCase.findByAsset(assetId), Review.class);
    }

    public Mono<ServerResponse> findByBuyer(ServerRequest request) {
        UUID buyerId = extractUserId(request);
        return ServerResponse.ok()
            .body(getReviewsUseCase.findByBuyer(buyerId), Review.class);
    }

    public Mono<ServerResponse> hide(ServerRequest request) {
        String reviewId = request.pathVariable("id");
        return request.bodyToMono(HideReviewRequest.class)
            .flatMap(body -> moderateReviewUseCase.hide(
                new ModerateReviewUseCase.HideCommand(reviewId, body.reason())))
            .flatMap(r -> ServerResponse.ok().bodyValue(r));
    }

    public Mono<ServerResponse> restore(ServerRequest request) {
        String reviewId = request.pathVariable("id");
        return moderateReviewUseCase.restore(reviewId)
            .flatMap(r -> ServerResponse.ok().bodyValue(r));
    }

    private UUID extractUserId(ServerRequest request) {
        return UUID.fromString(request.headers().firstHeader("X-User-Id"));
    }
}
