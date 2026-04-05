package com.reactivecommerce.payment.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configura el WebClient para comunicarse con el Payment Gateway externo.
 *
 * Timeouts conservadores — el gateway de pagos es un servicio externo:
 *  connect: 3s  → tolerancia menor que Order Service (externo, no interno)
 *  read:    10s → algunos gateways tardan en confirmar el cobro
 *  write:   5s
 *
 * Resilience4j (Circuit Breaker en ProcessPaymentUseCaseImpl) maneja
 * los fallos a nivel de lógica de negocio.
 */
@Configuration
public class WebClientConfig {

    @Value("${payment.gateway.base-url:https://api.stripe.com}")
    private String gatewayBaseUrl;

    @Bean("gatewayWebClient")
    public WebClient gatewayWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(5,  TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(gatewayBaseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
