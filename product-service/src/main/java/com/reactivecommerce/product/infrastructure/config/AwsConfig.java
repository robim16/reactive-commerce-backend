package com.reactivecommerce.product.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configuración AWS del Product Service.
 *
 * endpointOverride:
 *   Si aws.endpoint está definido (ej: http://localstack:4566 en Docker),
 *   ambos clientes apuntan a LocalStack en lugar de AWS real.
 *   En producción dejar AWS_ENDPOINT vacío → usa AWS real.
 *
 * forcePathStyle en S3AsyncClient:
 *   LocalStack requiere path-style URLs (http://host/bucket/key)
 *   en lugar de virtual-hosted (http://bucket.host/key).
 *   En producción AWS soporta ambos estilos.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key:local}")
    private String accessKey;

    @Value("${aws.secret-key:local}")
    private String secretKey;

    @Value("${aws.endpoint:}")
    private String endpoint;

    @Bean
    public S3AsyncClient s3AsyncClient() {
        var builder = S3AsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentials())
            .forcePathStyle(true); // requerido por LocalStack

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(credentials());

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey));
    }
}
