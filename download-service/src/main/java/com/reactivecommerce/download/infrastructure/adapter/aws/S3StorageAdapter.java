package com.reactivecommerce.download.infrastructure.adapter.aws;

import com.reactivecommerce.download.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

/**
 * Adaptador S3 que implementa StoragePort.
 * Genera presigned URLs de descarga con TTL de 15 minutos.
 * El archivo viaja directamente de S3 al buyer sin pasar por el backend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageAdapter implements StoragePort {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public Mono<String> generateDownloadPresignedUrl(String s3Key, Duration ttl) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getRequest)
                .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Presigned URL generated for key={} ttl={}min", s3Key, ttl.toMinutes());
            return url;
        });
    }
}
