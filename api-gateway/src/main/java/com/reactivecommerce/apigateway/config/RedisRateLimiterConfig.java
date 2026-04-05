package com.reactivecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configuración del Rate Limiter basado en Redis para Spring Cloud Gateway.
 *
 * Spring Cloud Gateway requiere un bean KeyResolver para saber cómo identificar
 * al cliente y aplicar los límites por separado. Sin este bean la app arranca
 * pero el RequestRateLimiter lanza NoSuchBeanDefinitionException en runtime.
 *
 * Estrategias de KeyResolver implementadas:
 *
 * 1. ipKeyResolver (activo en auth routes):
 *    Limita por IP. Protege los endpoints de login/registro contra brute-force
 *    y ataques de fuerza bruta. Configurado en application.yml con:
 *      replenishRate: 10   (10 requests/s en estado estable)
 *      burstCapacity:  20  (hasta 20 requests en ráfaga)
 *
 * 2. userKeyResolver (disponible para rutas autenticadas):
 *    Limita por X-User-Id (inyectado por JwtAuthFilter). Más preciso que por IP
 *    en redes compartidas (NAT, proxies). Se puede activar en rutas específicas.
 *    Si el header no está presente (request no autenticada) cae back a "anonymous".
 *
 * El bean principal (@Primary) es ipKeyResolver porque la configuración actual
 * en application.yml solo aplica rate limiting a /api/v1/auth/** (rutas públicas).
 */
@Configuration
public class RedisRateLimiterConfig {

    /**
     * Rate limiting por IP — usado en rutas públicas (/api/v1/auth/**).
     * Identifica al cliente por su dirección IP remota.
     * Usa X-Forwarded-For si el gateway está detrás de un load balancer.
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

            String ip = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()  // primer IP de la cadena de proxies
                : exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";

            return Mono.just(ip);
        };
    }

    /**
     * Rate limiting por usuario autenticado — disponible para rutas protegidas.
     * Requiere que JwtAuthFilter se aplique antes en la cadena de filtros.
     * Para activar en una ruta añadir:
     *   args:
     *     key-resolver: "#{@userKeyResolver}"
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }
}
