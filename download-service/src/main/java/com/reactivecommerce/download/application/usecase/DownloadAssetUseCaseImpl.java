package com.reactivecommerce.download.application.usecase;

import com.reactivecommerce.download.domain.port.in.DownloadAssetUseCase;
import com.reactivecommerce.download.domain.port.out.DownloadTokenRepository;
import com.reactivecommerce.download.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.download.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadAssetUseCaseImpl implements DownloadAssetUseCase {

    private final DownloadTokenRepository tokenRepository;
    private final StoragePort storagePort;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Result> execute(Command command) {
        return tokenRepository.findById(command.tokenId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Token not found")))
            .flatMap(token -> {
                if (!token.canDownload()) {
                    String reason = token.revoked() ? "Token revocado"
                        : token.downloadCount() >= token.maxDownloads() ? "L\u00edmite de descargas alcanzado"
                        : "Token expirado";
                    return Mono.error(new IllegalStateException(reason));
                }
                return tokenRepository.update(token.incrementCount())
                    .flatMap(updated -> storagePort
                        .generateDownloadPresignedUrl(updated.s3Key(), Duration.ofMinutes(15))
                        .flatMap(url -> eventPublisher
                            .publish("download.requested", updated.id(),
                                Map.of("tokenId", updated.id(), "buyerId", updated.buyerId(),
                                       "assetId", updated.assetId()))
                            .thenReturn(new Result(url, updated.s3Key()))
                        )
                    );
            });
    }
}
