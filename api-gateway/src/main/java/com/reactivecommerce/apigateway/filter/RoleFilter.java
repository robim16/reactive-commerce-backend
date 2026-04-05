package com.reactivecommerce.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Filtro de control de acceso por rol (RBAC).
 *
 * Se aplica DESPUÉS de JwtAuthFilter, que ya propagó X-User-Role como header interno.
 * Verifica que el rol del usuario autenticado esté en la lista de roles permitidos
 * configurada en application.yml para cada ruta.
 *
 * Configuración en application.yml:
 *   filters:
 *     - name: RoleFilter
 *       args:
 *         allowedRoles: ADMIN,CREATOR
 *
 * Si X-User-Role no está presente (request no pasó por JwtAuthFilter primero)
 * se devuelve 403 FORBIDDEN. Si el rol no está en la lista → 403 FORBIDDEN.
 *
 * Roles del sistema: ADMIN, CREATOR, BUYER, MODERATOR.
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
                log.warn("RoleFilter: X-User-Role header missing — possible filter ordering issue");
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

    public static class Config {
        /** Comma-separated list of allowed roles, e.g. "ADMIN,CREATOR" */
        private String allowedRoles = "";

        public String getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(String allowedRoles) { this.allowedRoles = allowedRoles; }
    }
}
