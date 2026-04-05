package com.reactivecommerce.report.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * RouterRest — entrypoint web del Report Service.
 */
@Configuration
@RequiredArgsConstructor
public class ReportRouterRest {

    private final ReportHandler handler;

    @Bean
    public RouterFunction<ServerResponse> reportRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/reports"), builder -> builder
                .POST("",      accept(APPLICATION_JSON), handler::request)
                .GET("/{id}",                            handler::getById)
            )
            .build();
    }
}
