package com.reactivecommerce.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de Spring Security para el API Gateway.
 *
 * El gateway usa Spring Cloud Gateway (WebFlux) para el enrutamiento.
 * La validación JWT real la hace JwtAuthFilter (GatewayFilter) en las
 * rutas que lo requieren, no Spring Security.
 *
 * Esta configuración deshabilita la seguridad automática de Spring Security
 * (que por defecto bloquea todas las requests) y delega toda la autenticación
 * a los filtros de gateway configurados en application.yml.
 *
 * Motivos para deshabilitar la seguridad HTTP de Spring Security aquí:
 *   1. Spring Cloud Gateway ya gestiona el flujo de requests con sus propios filtros.
 *   2. Tener dos capas de seguridad causaría conflictos de orden y doble validación.
 *   3. JwtAuthFilter es más flexible: puede aplicarse selectivamente por ruta.
 *   4. Las rutas públicas (/api/v1/auth/**) no deben ser interceptadas por Security.
 *
 * CORS: gestionado en application.yml (globalcors) para centralizar la config.
 * CSRF: deshabilitado porque el gateway es stateless (JWT, no cookies de sesión).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Actuator health endpoint: accesible sin autenticación (Kubernetes liveness/readiness)
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                // Todo lo demás pasa sin restricción de Spring Security.
                // JwtAuthFilter en las rutas que lo necesitan se encarga del JWT.
                .anyExchange().permitAll()
            )
            .build();
    }
}
