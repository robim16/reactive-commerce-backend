package com.reactivecommerce.download.infrastructure.config;

import com.reactivecommerce.download.infrastructure.adapter.persistence.DownloadTokenItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configuración de clientes AWS para el Download Service:
 *  - DynamoDbEnhancedAsyncClient  → tokens de descarga
 *  - DynamoDbAsyncTable           → tabla "download-tokens" con TTL y GSI
 *  - S3Presigner                  → presigned URLs de descarga
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key:local}")
    private String accessKey;

    @Value("${aws.secret-key:local}")
    private String secretKey;

    @Value("${aws.dynamodb.table:download-tokens}")
    private String tableName;

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return DynamoDbAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient(DynamoDbAsyncClient client) {
        return DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(client)
            .build();
    }

    @Bean
    public DynamoDbAsyncTable<DownloadTokenItem> downloadTokenTable(
            DynamoDbEnhancedAsyncClient enhancedClient) {
        return enhancedClient.table(tableName, TableSchema.fromBean(DownloadTokenItem.class));
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
