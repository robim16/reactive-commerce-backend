package com.reactivecommerce.order.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * RouterRest — entrypoint web del Order Service.
 */
@Configuration
@RequiredArgsConstructor
public class OrderRouterRest {

    private final OrderHandler handler;

    @Bean
    public RouterFunction<ServerResponse> orderRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/orders"), builder -> builder
                .POST("",               accept(APPLICATION_JSON), handler::create)
                .GET("",                                          handler::list)
                .GET("/{id}",                                     handler::findById)
                .POST("/{id}/refund",   accept(APPLICATION_JSON), handler::refund)
            )
            .build();
    }
}
