package com.reactivecommerce.apigateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Filtro RBAC del API Gateway.
 *
 * Se aplica DESPUÉS de JwtAuthFilter, que ya propagó X-User-Role.
 * Verifica que el rol del usuario esté en la lista de roles permitidos
 * configurada en application.yml para cada ruta.
 *
 * Si X-User-Role falta (JwtAuthFilter con required=false en ruta pública)
 * y la ruta requiere rol específico, devuelve 403.
 *
 * Configuración:
 *   filters:
 *     - name: RoleFilter
 *       args:
 *         allowedRoles: ADMIN,CREATOR
 */
@Slf4j
@Component
public class RoleFilter extends AbstractGatewayFilterFactory<RoleFilter.Config> {

    public RoleFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        List<String> allowed = Arrays.asList(config.getAllowedRoles().split(","));

        return (exchange, chain) -> {
            String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");

            if (role == null || role.isBlank()) {
                log.warn("RoleFilter: X-User-Role missing — denied");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            if (!allowed.contains(role.trim())) {
                log.warn("RoleFilter: role='{}' not in allowedRoles={}", role, allowed);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    @Getter
    @Setter
    public static class Config {
        private String allowedRoles = "";
    }
}
