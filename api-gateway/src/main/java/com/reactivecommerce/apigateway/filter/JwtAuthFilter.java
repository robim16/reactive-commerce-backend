package com.reactivecommerce.apigateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Filtro JWT del API Gateway.
 *
 * Soporta dos modos configurables desde application.yml:
 *
 *   required: true  (default) — el token es obligatorio.
 *     Si falta o es inválido → 401. Usado en rutas privadas
 *     (/orders, /payments, /downloads, /reviews, /notifications, /reports).
 *
 *   required: false — el token es opcional.
 *     Si falta → continúa sin headers de usuario (ruta pública).
 *     Si está presente y es válido → propaga X-User-Id, X-User-Role, X-User-Email.
 *     Si está presente y es inválido → 401 (token corrupto no se ignora).
 *     Usado en rutas del catálogo (/assets/**) donde los visitantes anónimos
 *     pueden ver assets pero los autenticados reciben contenido personalizado.
 *
 * Configuración en application.yml:
 *   filters:
 *     - name: JwtAuthFilter
 *       args:
 *         required: false   # opcional para rutas públicas
 */
@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final SecretKey secretKey;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        super(Config.class);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                .getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Sin token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                if (config.isRequired()) {
                    log.debug("Missing Authorization header on protected route");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                // Ruta pública sin token — continuar sin contexto de usuario
                return chain.filter(exchange);
            }

            // Con token — validar siempre, independientemente de required
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();

                // Verificar que sea un access token, no un refresh token
                String type = claims.get("type", String.class);
                if (!"access".equals(type)) {
                    log.warn("Non-access token used on API: type={}", type);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String userId = claims.getSubject();
                String role   = claims.get("role",  String.class);
                String email  = claims.get("email", String.class);

                var mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Role",  role)
                    .header("X-User-Email", email)
                    .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (JwtException e) {
                log.warn("Invalid JWT: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    @Getter
    @Setter
    public static class Config {
        /** Si es true, el token es obligatorio. Default: true. */
        private boolean required = true;
    }
}
