package com.reactivecommerce.download.infrastructure.entrypoint.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * RouterRest — entrypoint web del Download Service.
 *
 * Rutas expuestas:
 *
 *  GET  /api/v1/downloads/{tokenId}
 *       Valida el token y redirige (302) a la presigned URL de S3.
 *       Buyer usa este link para descargar el asset directamente desde S3.
 *
 *  GET  /api/v1/downloads/library
 *       Biblioteca del buyer: lista todos sus tokens (HU-DWN-02).
 *       Devuelve estado de cada token (descargas usadas, máximas, expiración).
 *
 *  GET  /api/v1/downloads/orders/{orderId}
 *       Estado del token de una orden específica.
 *       Usado por el frontend para mostrar el botón de descarga en el historial.
 *
 *  POST /api/v1/downloads/orders/{orderId}/regenerate
 *       Regenera el token expirado de una orden (HU-DWN-02, máx 3 veces).
 *       Devuelve el nuevo token con TTL renovado.
 */
@Configuration
@RequiredArgsConstructor
public class DownloadRouterRest {

    private final DownloadHandler handler;

    @Bean
    public RouterFunction<ServerResponse> downloadRoutes() {
        return RouterFunctions.route()
                .nest(path("/api/v1/downloads"), builder -> builder
                        // Biblioteca del buyer
                        .GET("/library",                                              handler::library)
                        // Token por orden (para historial de pedidos)
                        .GET("/orders/{orderId}",                                     handler::tokenByOrder)
                        // Regenerar token expirado
                        .POST("/orders/{orderId}/regenerate", accept(APPLICATION_JSON), handler::regenerate)
                        // Descargar asset — redirige a S3 presigned URL
                        .GET("/{tokenId}",                                            handler::download)
                )
                .build();
    }
}
