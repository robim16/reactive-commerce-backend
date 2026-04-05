package com.reactivecommerce.product.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * RouterRest — entrypoint web del Product Service.
 * Estilo funcional Spring WebFlux. Sin @RestController, sin @RequestMapping.
 */
@Configuration
@RequiredArgsConstructor
public class ProductRouterRest {

    private final ProductHandler handler;

    @Bean
    public RouterFunction<ServerResponse> productRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/assets"), builder -> builder
                // Catálogo público
                .GET("",                                                          handler::search)
                .GET("/{id}",                                                     handler::findById)
                // Creator
                .POST("",               accept(APPLICATION_JSON),                 handler::create)
                .PUT("/{id}/publish",                                             handler::publish)
                .PUT("/{id}/unpublish",                                           handler::unpublish)
                .PUT("/{id}",           accept(APPLICATION_JSON),                 handler::update)
                .DELETE("/{id}",                                                  handler::delete)
                // Moderación
                .POST("/{id}/approve",  accept(APPLICATION_JSON),                 handler::approve)
                .POST("/{id}/reject",   accept(APPLICATION_JSON),                 handler::reject)
                // Presigned URLs
                .GET("/{id}/download-url",                                        handler::downloadUrl)
                .GET("/{id}/preview-url",                                         handler::previewUrl)
            )
            .build();
    }
}
