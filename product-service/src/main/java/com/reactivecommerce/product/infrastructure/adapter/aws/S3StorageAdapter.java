package com.reactivecommerce.product.infrastructure.adapter.aws;

import com.reactivecommerce.product.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageAdapter implements StoragePort {

    private final S3Presigner s3Presigner;
    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public Mono<String> generateUploadPresignedUrl(String s3Key, Duration ttl) {
        return Mono.fromCallable(() -> {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket).key(s3Key).build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl).putObjectRequest(putRequest).build();
            return s3Presigner.presignPutObject(presignRequest).url().toString();
        });
    }

    @Override
    public Mono<String> generateDownloadPresignedUrl(String s3Key, Duration ttl) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket).key(s3Key).build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl).getObjectRequest(getRequest).build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        });
    }

    @Override
    public Mono<Void> deleteObject(String s3Key) {
        return Mono.fromFuture(s3AsyncClient.deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build()
        )).then();
    }
}
