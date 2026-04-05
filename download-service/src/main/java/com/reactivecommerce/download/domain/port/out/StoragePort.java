package com.reactivecommerce.download.domain.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

public interface StoragePort {
    Mono<String> generateDownloadPresignedUrl(String s3Key, Duration ttl);
}
