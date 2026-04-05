# ReactiveCommerce Platform — Backend

Marketplace de assets digitales construido sobre arquitectura de microservicios reactivos.

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Runtime | Java 21 + Spring Boot 3.3 |
| Web | Spring WebFlux (RouterFunction + HandlerFunction) |
| Persistencia SQL | R2DBC + PostgreSQL (reactivo no bloqueante) |
| Persistencia NoSQL | Spring Data MongoDB Reactive |
| Cache | Spring Data Redis Reactive (Lettuce) |
| Mensajería | Reactor Kafka (producers + consumers reactivos) |
| Resiliencia | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| Service Discovery | Spring Cloud Netflix Eureka |
| Config centralizada | Spring Cloud Config Server |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Observabilidad | Micrometer + Prometheus + Grafana + Zipkin + Loki |
| Cloud | AWS S3, SES, SQS, DynamoDB, Lambda, CloudFront |
| Contenedores | Docker + Docker Compose |

## Microservicios

| Servicio | Puerto | Descripción |
|---------|--------|-------------|
| config-service | 8888 | Configuración centralizada |
| eureka-service | 8761 | Service discovery |
| api-gateway | 8080 | Punto de entrada único, JWT, rate limiting |
| auth-service | 8081 | Registro, login, JWT, OAuth2 |
| product-service | 8082 | Catálogo de assets, moderación |
| order-service | 8083 | Pedidos con Event Sourcing + Saga |
| payment-service | 8084 | Pagos con Circuit Breaker |
| download-service | 8085 | Tokens de descarga seguros (DynamoDB TTL) |
| review-service | 8086 | Valoraciones en MongoDB |
| notification-service | 8087 | SSE en tiempo real + SES emails |
| report-service | 8088 | Generación asíncrona de informes |

## Arquitectura Hexagonal (Bancolobia Scaffold)

Cada microservicio sigue estrictamente la estructura:

```
src/main/java/com/reactivecommerce/{service}/
├── domain/
│   ├── model/              ← Entidades, value objects, agregados
│   └── port/
│       ├── in/             ← UseCase interfaces (comandos/queries)
│       └── out/            ← Repository/Gateway/Publisher ports
├── application/
│   └── usecase/            ← Implementaciones de use cases
└── infrastructure/
    ├── entrypoint/
    │   ├── web/
    │   │   ├── *RouterRest.java   ← @Configuration + RouterFunction
    │   │   ├── *Handler.java      ← HandlerFunction (sin @Controller)
    │   │   └── dto/               ← Records de request/response
    │   └── kafka/
    │       └── *KafkaConsumer.java
    ├── adapter/
    │   ├── persistence/    ← R2DBC / MongoDB / DynamoDB
    │   ├── kafka/          ← KafkaSender producers
    │   └── aws/            ← S3, SES, SQS adapters
    └── config/             ← Beans de configuración
```

### Patrón RouterRest + Handler (Estilo Bancolobia)

```java
// RouterRest.java — solo define rutas
@Configuration
public class ProductRouterRest {
    @Bean
    public RouterFunction<ServerResponse> productRoutes() {
        return RouterFunctions.route()
            .nest(path("/api/v1/assets"), builder -> builder
                .GET("",   handler::search)
                .POST("",  handler::create)
                .GET("/{id}", handler::findById)
            ).build();
    }
}

// Handler.java — lógica HTTP, sin @RestController ni @RequestMapping
@Component
public class ProductHandler {
    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateAssetRequest.class)
            .flatMap(body -> createAssetUseCase.execute(...))
            .flatMap(result -> ServerResponse.status(201).bodyValue(result));
    }
}
```

## Tópicos Kafka

| Tópico | Productor | Consumidores |
|--------|-----------|-------------|
| asset.uploaded | Product | Product, Notification |
| asset.approved | Product | Notification, Report |
| asset.rejected | Product | Notification |
| asset.published | Product | Notification, Report |
| order.created | Order | Payment, Notification |
| order.payment_completed | Payment | Order, Download, Notification |
| order.payment_failed | Payment | Order, Notification |
| order.completed | Order | Report, Notification |
| order.refunded | Order | Download, Notification, Report |
| download.requested | Download | Report |
| review.created | Review | Product, Notification |
| report.requested | Report | Report (self-consume) |
| user.registered | Auth | Notification |

## Saga de Compra (Coreografía)

```
Buyer → POST /api/v1/orders
  └─ Order Service: crea PENDING, publica order.created
       └─ Payment Service: consume order.created
            ├─ [OK]  publica order.payment_completed
            │    └─ Order Service: confirma → COMPLETED, publica order.completed
            │         └─ Download Service: genera token DynamoDB (TTL 24h)
            │         └─ Notification: SSE + email al buyer y creator
            └─ [FAIL] publica order.payment_failed
                 └─ Order Service: marca FAILED
                 └─ Notification: alerta al buyer
```

## Inicio Rápido

```bash
# Levantar todo el ecosistema
docker-compose up -d

# Orden automático por healthchecks:
# Zookeeper → Kafka → Postgres/MongoDB/Redis → Config → Eureka
# → Microservicios de negocio → API Gateway

# Accesos:
# API Gateway:    http://localhost:8080
# Eureka:         http://localhost:8761
# Kafka UI:       http://localhost:9093
# Prometheus:     http://localhost:9090
# Grafana:        http://localhost:3001  (admin/admin)
# Zipkin:         http://localhost:9411
```

## Variables de Entorno Clave

| Variable | Descripción | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Clave HMAC-SHA256 para JWT | `reactive-commerce-secret-key...` |
| `AWS_ACCESS_KEY_ID` | Credenciales AWS | `local` |
| `AWS_SECRET_ACCESS_KEY` | Credenciales AWS | `local` |
| `AWS_S3_BUCKET` | Bucket de assets | `reactive-commerce-assets` |
| `PAYMENT_COMMISSION_RATE` | Comisión plataforma | `0.20` (20%) |
| `DOWNLOAD_MAX_DOWNLOADS` | Descargas por token | `5` |

## Patrones Reactivos Implementados

| Patrón | Dónde | Propósito |
|--------|-------|-----------|
| `onBackpressureBuffer` | Product | Ingesta masiva sin perder eventos |
| `Sinks.Many` (hot publisher) | Notification | SSE compartido por usuario |
| `Flux.interval` | Order | Job de expiración de pedidos |
| `Schedulers.boundedElastic` | Report | PDF en pool dedicado (no bloquea event loop) |
| `takeUntilOther` | Download | Cancelar token si pedido se revierte |
| `concatMap` | Order | Eventos de pedido en orden estricto |
| `flatMap` paralelo | Product | Upload de múltiples fotos en paralelo |
| `share()` / `publish()` | Notification | Un Flux SSE, múltiples suscriptores |
| Circuit Breaker | Payment | Protege el gateway de pagos |
| Retry + backoff exponencial | Payment, AWS | Reintentos inteligentes |
| Bulkhead | Product | Máx 10 llamadas simultáneas a S3 |
