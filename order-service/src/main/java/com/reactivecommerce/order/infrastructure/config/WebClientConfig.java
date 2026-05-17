package com.reactivecommerce.order.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configura el WebClient que AssetPriceAdapter usa para consultar
 * el Product Service.
 *
 * @LoadBalanced: integración con Eureka — lb://product-service resuelve
 * dinámicamente las instancias disponibles sin hardcodear URLs.
 *
 * Timeouts:
 *  - connect: 2s  → falla rápido si el servicio no está disponible
 *  - read:    5s  → alineado con el timeout de Resilience4j
 *  - write:   5s
 *
 * El Circuit Breaker en AssetPriceAdapter envuelve las llamadas;
 * estos timeouts son la segunda línea de defensa.
 */
@Configuration
public class WebClientConfig {

    @Value("${services.product-service.base-url:lb://product-service}")
    private String productServiceBaseUrl;

    // 1. Builder con Load Balanced
    @Bean
    @LoadBalanced
    public WebClient.Builder productServiceWebClientBuilder() {
        return WebClient.builder();
    }

    // 2. WebClient construido a partir del builder anterior
    @Bean("productServiceClient")
    public WebClient productServiceClient(WebClient.Builder productServiceWebClientBuilder) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        return productServiceWebClientBuilder
                .baseUrl(productServiceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
