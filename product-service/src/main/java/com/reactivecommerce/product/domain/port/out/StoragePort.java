package com.reactivecommerce.product.domain.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

public interface StoragePort {
    Mono<String> generateUploadPresignedUrl(String s3Key, Duration ttl);
    Mono<String> generateDownloadPresignedUrl(String s3Key, Duration ttl);
    Mono<Void> deleteObject(String s3Key);
}
