package com.reactivecommerce.download.application.usecase;

import com.reactivecommerce.download.domain.port.in.RevokeTokenUseCase;
import com.reactivecommerce.download.domain.port.out.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Implementación del caso de uso de revocación de token.
 * Se invoca desde el DownloadKafkaConsumer al consumir order.refunded.
 *
 * Flujo:
 *  1. Localiza el token activo de la orden vía GSI orderId-index en DynamoDB.
 *  2. Lo marca como revoked=true.
 *  3. Si no existe token (orden sin descarga generada aún), termina sin error —
 *     es un caso válido si el reembolso ocurre antes de que el token se genere.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeTokenUseCaseImpl implements RevokeTokenUseCase {

    private final DownloadTokenRepository tokenRepository;

    @Override
    public Mono<Void> execute(UUID orderId) {
        return tokenRepository.revoke(orderId)
            .doOnSuccess(v -> log.info("Token revoked for orderId={}", orderId))
            .onErrorResume(e -> {
                // Si no existe el token, no es un error de negocio
                log.warn("No token found to revoke for orderId={}: {}", orderId, e.getMessage());
                return Mono.empty();
            });
    }
}
