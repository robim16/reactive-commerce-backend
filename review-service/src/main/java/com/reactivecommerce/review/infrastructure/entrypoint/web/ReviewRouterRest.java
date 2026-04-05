package com.reactivecommerce.review.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * RouterRest — entrypoint web del Review Service.
 */
@Configuration
@RequiredArgsConstructor
public class ReviewRouterRest {

    private final ReviewHandler handler;

    @Bean
    public RouterFunction<ServerResponse> reviewRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/reviews"), builder -> builder
                .POST("",                accept(APPLICATION_JSON), handler::create)
                .GET("/asset/{assetId}",                          handler::findByAsset)
                .GET("/buyer",                                     handler::findByBuyer)
                .PUT("/{id}/hide",       accept(APPLICATION_JSON), handler::hide)
                .PUT("/{id}/restore",                              handler::restore)
            )
            .build();
    }
}
