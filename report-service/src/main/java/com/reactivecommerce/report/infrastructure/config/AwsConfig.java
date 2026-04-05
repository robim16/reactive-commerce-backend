package com.reactivecommerce.report.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configura los clientes AWS del Report Service:
 *
 * LambdaAsyncClient:
 *   Usado por LambdaReportGeneratorAdapter para invocar la función Lambda
 *   que genera los PDFs y CSVs de informes. El cliente asíncrono devuelve
 *   CompletableFuture compatible con Mono.fromFuture().
 *
 * S3AsyncClient:
 *   Usado para operaciones de escritura/lectura en S3 si fueran necesarias
 *   desde el Report Service directamente (actualmente Lambda escribe en S3).
 *
 * S3Presigner:
 *   Genera presigned URLs de descarga para los informes generados.
 *   TTL de 24h (configurable). Las URLs se incluyen en el evento report.ready
 *   para que el Notification Service las envíe al solicitante.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key:local}")
    private String accessKey;

    @Value("${aws.secret-key:local}")
    private String secretKey;

    @Bean
    public LambdaAsyncClient lambdaAsyncClient() {
        return LambdaAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .build();
    }
}
