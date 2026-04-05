package com.reactivecommerce.download.application.usecase;

import com.reactivecommerce.download.domain.model.DownloadToken;
import com.reactivecommerce.download.domain.port.in.GetLibraryUseCase;
import com.reactivecommerce.download.domain.port.out.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetLibraryUseCaseImpl implements GetLibraryUseCase {

    private final DownloadTokenRepository tokenRepository;

    @Override
    public Flux<DownloadToken> findByBuyer(UUID buyerId) {
        return tokenRepository.findByBuyerId(buyerId);
    }

    @Override
    public Mono<DownloadToken> findByOrder(UUID orderId) {
        return tokenRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No download token found for orderId=" + orderId)));
    }
}
