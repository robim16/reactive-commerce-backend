package com.reactivecommerce.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de Spring Security para el Auth Service.
 *
 * RESPONSABILIDADES EN ESTE SERVICIO:
 *   Spring Security se usa ÚNICAMENTE para BCryptPasswordEncoder.
 *   La autenticación y autorización de cada request la gestiona el
 *   API Gateway (JwtAuthFilter + RoleFilter) antes de que el request
 *   llegue aquí. El auth-service no debe volver a validar el token —
 *   confía en los headers X-User-Id y X-User-Role que el gateway propaga.
 *
 * POR QUÉ permitAll() EN TODAS LAS RUTAS:
 *   Si se usa .anyExchange().authenticated(), Spring Security intenta
 *   autenticar el request con su propio contexto (no tiene ReactiveAuthenticationManager
 *   configurado para JWT) → 401 en todas las rutas que no sean las del permitAll explícito.
 *   El control de acceso real está en:
 *     1. API Gateway: JwtAuthFilter valida el JWT, RoleFilter verifica el rol.
 *     2. AuthHandler.listUsers(): segunda línea de defensa sobre X-User-Role.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Todas las rutas son permitidas a nivel de Spring Security.
                // La autenticación real la hace el API Gateway antes de llegar aquí.
                .anyExchange().permitAll()
            )
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
