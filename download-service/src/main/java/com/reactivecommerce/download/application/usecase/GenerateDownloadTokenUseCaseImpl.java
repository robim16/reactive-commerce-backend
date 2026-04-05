package com.reactivecommerce.download.application.usecase;

import com.reactivecommerce.download.domain.model.DownloadToken;
import com.reactivecommerce.download.domain.port.in.GenerateDownloadTokenUseCase;
import com.reactivecommerce.download.domain.port.out.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GenerateDownloadTokenUseCaseImpl implements GenerateDownloadTokenUseCase {

    private final DownloadTokenRepository tokenRepository;

    @Value("${download.max-downloads:5}")
    private int maxDownloads;

    @Override
    public Mono<DownloadToken> execute(Command command) {
        DownloadToken token = DownloadToken.create(
            command.orderId(), command.buyerId(),
            command.assetId(), command.s3Key(), maxDownloads
        );
        return tokenRepository.save(token);
    }
}
