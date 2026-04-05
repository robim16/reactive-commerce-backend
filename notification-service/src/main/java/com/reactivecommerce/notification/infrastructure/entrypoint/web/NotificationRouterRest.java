package com.reactivecommerce.notification.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * RouterRest — entrypoint web del Notification Service.
 * Expone el endpoint SSE y los endpoints REST de notificaciones.
 */
@Configuration
@RequiredArgsConstructor
public class NotificationRouterRest {

    private final NotificationHandler handler;

    @Bean
    public RouterFunction<ServerResponse> notificationRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/notifications"), builder -> builder
                .GET("/stream",    handler::stream)      // SSE endpoint
                .GET("",           handler::list)
                .PUT("/read-all",  handler::markAllRead)
                .PUT("/{id}/read", handler::markRead)
            )
            .build();
    }
}
