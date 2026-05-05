package com.reactivecommerce.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway — punto de entrada único para todos los microservicios.
 *
 * Responsabilidades:
 *   1. Enrutamiento reactivo a los microservicios vía Eureka (lb://).
 *   2. Validación JWT + propagación de X-User-Id, X-User-Role, X-User-Email.
 *   3. Control de acceso por rol (RoleFilter por ruta).
 *   4. Rate limiting con Redis (RequestRateLimiter en rutas públicas).
 *   5. CORS global para el frontend Next.js.
 *
 * No contiene lógica de negocio. No conecta a bases de datos propias.
 * Solo usa Redis para el rate limiter.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
