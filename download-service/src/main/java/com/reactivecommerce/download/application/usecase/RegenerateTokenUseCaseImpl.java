package com.reactivecommerce.download.application.usecase;

import com.reactivecommerce.download.domain.model.DownloadToken;
import com.reactivecommerce.download.domain.port.in.RegenerateTokenUseCase;
import com.reactivecommerce.download.domain.port.in.RevokeTokenUseCase;
import com.reactivecommerce.download.domain.port.out.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Regenera un token de descarga expirado para una orden ya completada (HU-DWN-02).
 *
 * Flujo:
 *  1. Localiza el token vigente de la orden.
 *  2. Verifica que el buyer sea el propietario.
 *  3. Rechaza si el token fue revocado por reembolso (no regenerable).
 *  4. Verifica que las regeneraciones previas no superen el máximo configurado.
 *  5. Delega la revocación del token anterior a RevokeTokenUseCase
 *     (evita duplicar lógica de revocación que también usa el Kafka consumer).
 *  6. Persiste el nuevo token con TTL y downloadCount reiniciados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegenerateTokenUseCaseImpl implements RegenerateTokenUseCase {

    private final DownloadTokenRepository tokenRepository;
    private final RevokeTokenUseCase revokeTokenUseCase;

    @Value("${download.max-regenerations:3}")
    private int maxRegenerations;

    @Value("${download.max-downloads:5}")
    private int maxDownloads;

    @Override
    public Mono<DownloadToken> execute(Command command) {
        return tokenRepository.findByOrderId(command.orderId())
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("No token found for orderId=" + command.orderId())))
            .flatMap(existing -> validateAndRegenerate(existing, command));
    }

    private Mono<DownloadToken> validateAndRegenerate(DownloadToken existing, Command command) {
        if (!existing.buyerId().equals(command.buyerId())) {
            return Mono.error(new IllegalStateException("Unauthorized"));
        }
        if (existing.revoked()) {
            return Mono.error(new IllegalStateException(
                "Token revocado por reembolso — no puede regenerarse"));
        }
        return tokenRepository.countRegenerationsByOrderId(command.orderId())
            .flatMap(count -> {
                if (count >= maxRegenerations) {
                    return Mono.error(new IllegalStateException(
                        "L\u00edmite de regeneraciones alcanzado (" + maxRegenerations + ")"));
                }
                DownloadToken newToken = DownloadToken.create(
                    existing.orderId(), existing.buyerId(),
                    existing.assetId(), existing.s3Key(),
                    maxDownloads
                );
                // Delega la revocación del token anterior al mismo use case
                // que usa el Kafka consumer, evitando duplicar la lógica
                return revokeTokenUseCase.execute(existing.orderId())
                    .then(tokenRepository.save(newToken))
                    .doOnSuccess(t -> log.info(
                        "Token regenerated for orderId={} regeneration={}/{}",
                        command.orderId(), count + 1, maxRegenerations));
            });
    }
}
