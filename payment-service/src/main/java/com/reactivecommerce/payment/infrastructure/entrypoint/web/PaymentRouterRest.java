package com.reactivecommerce.payment.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * RouterRest — entrypoint web del Payment Service.
 */
@Configuration
@RequiredArgsConstructor
public class PaymentRouterRest {

    private final PaymentHandler handler;

    @Bean
    public RouterFunction<ServerResponse> paymentRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/payments"), builder -> builder
                .GET("/orders/{orderId}", handler::getByOrder)
            )
            .build();
    }
}
