package com.reactivecommerce.download.domain.port.in;

import reactor.core.publisher.Mono;

public interface DownloadAssetUseCase {
    record Command(String tokenId, String buyerId) {}
    record Result(String presignedUrl, String filename) {}
    Mono<Result> execute(Command command);
}
