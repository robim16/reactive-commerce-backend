package com.reactivecommerce.auth.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
@RequiredArgsConstructor
public class AuthRouterRest {

    private final AuthHandler handler;

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/auth"), builder -> builder
                .POST("/register",   accept(APPLICATION_JSON), handler::register)
                .POST("/login",      accept(APPLICATION_JSON), handler::login)
                .POST("/refresh",    accept(APPLICATION_JSON), handler::refresh)
                .POST("/logout",     accept(APPLICATION_JSON), handler::logout)
                .GET("/verify-email",                          handler::verifyEmail)
                .GET("/me",                                    handler::me)
            )
            // Ruta separada de /auth — los usuarios no son un recurso de autenticación
            // GET /api/v1/users → solo ADMIN y MODERATOR (validado en handler + API Gateway)
            .GET("/api/v1/users", handler::listUsers)
            .build();
    }
}
