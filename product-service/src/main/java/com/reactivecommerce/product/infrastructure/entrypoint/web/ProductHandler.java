package com.reactivecommerce.product.infrastructure.entrypoint.web;

import com.reactivecommerce.product.domain.model.AssetCategory;
import com.reactivecommerce.product.domain.port.in.*;
import com.reactivecommerce.product.infrastructure.entrypoint.web.dto.UpdateAssetRequest;
import com.reactivecommerce.product.infrastructure.entrypoint.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handler reactivo del Product Service.
 * Transforma requests HTTP a comandos de dominio y construye respuestas reactivas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductHandler {

    private final CreateAssetUseCase createAssetUseCase;
    private final ModerateAssetUseCase moderateAssetUseCase;
    private final PublishAssetUseCase publishAssetUseCase;
    private final SearchAssetsUseCase searchAssetsUseCase;
    private final GeneratePresignedUrlUseCase presignedUrlUseCase;
    private final UpdateAssetUseCase updateAssetUseCase;
    private final DeleteAssetUseCase deleteAssetUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        UUID creatorId = extractUserId(request);
        return request.bodyToMono(CreateAssetRequest.class)
            .flatMap(body -> createAssetUseCase.execute(
                new CreateAssetUseCase.Command(
                    body.title(), body.description(),
                    AssetCategory.valueOf(body.category()),
                    body.tags(), new BigDecimal(body.price()),
                    body.license(), creatorId,
                    body.format(), body.fileSizeBytes()
                )
            ))
            .flatMap(session -> ServerResponse.status(HttpStatus.CREATED)
                .bodyValue(new UploadSessionResponse(session.assetId(), session.presignedUploadUrl())))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        var query = new SearchAssetsUseCase.Query(
            request.queryParam("q").orElse(null),
            request.queryParam("category").map(AssetCategory::valueOf).orElse(null),
            request.queryParam("minPrice").map(BigDecimal::new).orElse(null),
            request.queryParam("maxPrice").map(BigDecimal::new).orElse(null),
            request.queryParam("minRating").map(Double::parseDouble).orElse(null),
            request.queryParam("cursor").orElse(null),
            request.queryParam("size").map(Integer::parseInt).orElse(24),
            request.queryParam("sort")
                .map(SearchAssetsUseCase.SortBy::valueOf)
                .orElse(SearchAssetsUseCase.SortBy.NEWEST)
        );
        return ServerResponse.ok()
            .body(searchAssetsUseCase.search(query)
                .map(AssetSummaryResponse::from), AssetSummaryResponse.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return searchAssetsUseCase.findById(id)
            .flatMap(asset -> ServerResponse.ok().bodyValue(AssetDetailResponse.from(asset)))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> approve(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("id"));
        UUID moderatorId = extractUserId(request);
        return moderateAssetUseCase.approve(new ModerateAssetUseCase.ApproveCommand(assetId, moderatorId))
            .flatMap(asset -> ServerResponse.ok().bodyValue(AssetDetailResponse.from(asset)))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> reject(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("id"));
        UUID moderatorId = extractUserId(request);
        return request.bodyToMono(RejectRequest.class)
            .flatMap(body -> moderateAssetUseCase.reject(
                new ModerateAssetUseCase.RejectCommand(assetId, moderatorId, body.reason())))
            .flatMap(asset -> ServerResponse.ok().bodyValue(AssetDetailResponse.from(asset)));
    }

    public Mono<ServerResponse> publish(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("id"));
        UUID creatorId = extractUserId(request);
        return publishAssetUseCase.publish(assetId, creatorId)
            .flatMap(asset -> ServerResponse.ok().bodyValue(AssetDetailResponse.from(asset)))
            .onErrorResume(IllegalStateException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> unpublish(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("id"));
        UUID creatorId = extractUserId(request);
        return publishAssetUseCase.unpublish(assetId, creatorId)
            .flatMap(asset -> ServerResponse.ok().bodyValue(AssetDetailResponse.from(asset)));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        UUID assetId    = UUID.fromString(request.pathVariable("id"));
        UUID requesterId = extractUserId(request);
        return request.bodyToMono(UpdateAssetRequest.class)
            .flatMap(body -> updateAssetUseCase.execute(
                new UpdateAssetUseCase.Command(
                    assetId, requesterId,
                    body.title(), body.description(),
                    body.price(), body.tags(), body.license()
                )
            ))
            .flatMap(asset -> ServerResponse.ok().bodyValue(AssetDetailResponse.from(asset)))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.notFound().build())
            .onErrorResume(IllegalStateException.class, e ->
                ServerResponse.status(HttpStatus.FORBIDDEN)
                    .bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        UUID assetId     = UUID.fromString(request.pathVariable("id"));
        UUID requesterId = extractUserId(request);
        return deleteAssetUseCase.execute(
                new DeleteAssetUseCase.Command(assetId, requesterId))
            .then(ServerResponse.noContent().build())
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.notFound().build())
            .onErrorResume(IllegalStateException.class, e ->
                ServerResponse.status(HttpStatus.CONFLICT)
                    .bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> downloadUrl(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("id"));
        return presignedUrlUseCase.forDownload(assetId)
            .flatMap(url -> ServerResponse.ok().bodyValue(new PresignedUrlResponse(url)));
    }

    public Mono<ServerResponse> previewUrl(ServerRequest request) {
        UUID assetId = UUID.fromString(request.pathVariable("id"));
        return presignedUrlUseCase.forModerationPreview(assetId)
            .flatMap(url -> ServerResponse.ok().bodyValue(new PresignedUrlResponse(url)));
    }

    private UUID extractUserId(ServerRequest request) {
        return UUID.fromString(request.headers().firstHeader("X-User-Id"));
    }
}
