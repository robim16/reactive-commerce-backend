package com.reactivecommerce.download.infrastructure.entrypoint.web;

import com.reactivecommerce.download.domain.model.DownloadToken;
import com.reactivecommerce.download.domain.port.in.DownloadAssetUseCase;
import com.reactivecommerce.download.domain.port.in.GetLibraryUseCase;
import com.reactivecommerce.download.domain.port.in.RegenerateTokenUseCase;
import com.reactivecommerce.download.infrastructure.entrypoint.web.dto.ErrorResponse;
import com.reactivecommerce.download.infrastructure.entrypoint.web.dto.TokenStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

/**
 * Handler reactivo del Download Service.
 * Cubre todas las operaciones de descarga y biblioteca del buyer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadHandler {

    private final DownloadAssetUseCase downloadAssetUseCase;
    private final GetLibraryUseCase getLibraryUseCase;
    private final RegenerateTokenUseCase regenerateTokenUseCase;

    /**
     * GET /api/v1/downloads/{tokenId}
     * Valida el token y redirige (302 Found) a la presigned URL de S3 con TTL 15 min.
     * Si el token expiró o fue revocado retorna 410 Gone con mensaje claro.
     */
    public Mono<ServerResponse> download(ServerRequest request) {
        String tokenId = request.pathVariable("tokenId");
        String buyerId = request.headers().firstHeader("X-User-Id");

        return downloadAssetUseCase.execute(new DownloadAssetUseCase.Command(tokenId, buyerId))
                .flatMap(result -> ServerResponse
                        .status(HttpStatus.FOUND)
                        .location(URI.create(result.presignedUrl()))
                        .build())
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build())
                .onErrorResume(IllegalStateException.class, e ->
                        ServerResponse.status(HttpStatus.GONE)
                                .bodyValue(ErrorResponse.of(e.getMessage())));
    }

    /**
     * GET /api/v1/downloads/library
     * Devuelve todos los tokens del buyer (biblioteca de assets comprados).
     * Incluye estado de cada token: descargas usadas/máximas, expiración, revocado.
     */
    public Mono<ServerResponse> library(ServerRequest request) {
        UUID buyerId = extractUserId(request);
        return ServerResponse.ok()
                .body(
                        getLibraryUseCase.findByBuyer(buyerId)
                                .map(TokenStatusResponse::from),
                        TokenStatusResponse.class
                );
    }

    /**
     * GET /api/v1/downloads/orders/{orderId}
     * Estado del token asociado a una orden. Usado en el historial de pedidos
     * para mostrar si el link de descarga está activo, expirado o revocado.
     */
    public Mono<ServerResponse> tokenByOrder(ServerRequest request) {
        UUID orderId = UUID.fromString(request.pathVariable("orderId"));
        return getLibraryUseCase.findByOrder(orderId)
                .map(TokenStatusResponse::from)
                .flatMap(dto -> ServerResponse.ok().bodyValue(dto))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build());
    }

    /**
     * POST /api/v1/downloads/orders/{orderId}/regenerate
     * Regenera el token expirado de una orden ya completada.
     * Máximo 3 regeneraciones por asset (HU-DWN-02).
     * Devuelve el nuevo token con TTL renovado de 24 horas.
     */
    public Mono<ServerResponse> regenerate(ServerRequest request) {
        UUID orderId = UUID.fromString(request.pathVariable("orderId"));
        UUID buyerId = extractUserId(request);

        return regenerateTokenUseCase.execute(
                        new RegenerateTokenUseCase.Command(orderId, buyerId))
                .map(TokenStatusResponse::from)
                .flatMap(dto -> ServerResponse.status(HttpStatus.CREATED).bodyValue(dto))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build())
                .onErrorResume(IllegalStateException.class, e ->
                        ServerResponse.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .bodyValue(ErrorResponse.of(e.getMessage())));
    }

    private UUID extractUserId(ServerRequest request) {
        String header = request.headers().firstHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        return UUID.fromString(header);
    }
}
