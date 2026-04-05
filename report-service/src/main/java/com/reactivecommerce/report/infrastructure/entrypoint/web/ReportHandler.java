package com.reactivecommerce.report.infrastructure.entrypoint.web;

import com.reactivecommerce.report.domain.model.ReportType;
import com.reactivecommerce.report.domain.port.in.GetReportUseCase;
import com.reactivecommerce.report.domain.port.in.RequestReportUseCase;
import com.reactivecommerce.report.infrastructure.entrypoint.web.dto.ErrorResponse;
import com.reactivecommerce.report.infrastructure.entrypoint.web.dto.ReportRequest;
import com.reactivecommerce.report.infrastructure.entrypoint.web.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Handler reactivo del Report Service.
 *
 * POST /api/v1/reports        → solicita un informe (async, retorna 202 + reportId)
 * GET  /api/v1/reports/{id}   → consulta el estado (frontend hace polling RTK Query)
 *
 * El frontend usa pollingInterval dinámico:
 *   - Mientras status == PENDING o PROCESSING → poll cada 3s
 *   - Cuando status == READY o FAILED         → detiene el polling
 *
 * La respuesta incluye progressPercent (0-100) para mostrar una barra de progreso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportHandler {

    private final RequestReportUseCase requestReportUseCase;
    private final GetReportUseCase     getReportUseCase;

    /**
     * POST /api/v1/reports
     * Solicita la generación de un informe.
     * Responde con 202 Accepted + ReportResponse{status=PENDING} inmediatamente.
     * La generación continúa en background.
     */
    public Mono<ServerResponse> request(ServerRequest request) {
        UUID userId = extractUserId(request);
        return request.bodyToMono(ReportRequest.class)
            .flatMap(body -> {
                try {
                    return requestReportUseCase.execute(
                        new RequestReportUseCase.Command(
                            userId,
                            ReportType.valueOf(body.type()),
                            body.periodFrom() != null ? Instant.parse(body.periodFrom()) : null,
                            body.periodTo()   != null ? Instant.parse(body.periodTo())   : null
                        )
                    );
                } catch (IllegalArgumentException e) {
                    return Mono.error(e);
                }
            })
            .map(ReportResponse::from)
            .flatMap(resp -> ServerResponse.status(HttpStatus.ACCEPTED).bodyValue(resp))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())))
            .onErrorResume(e -> {
                log.error("Error requesting report for userId={}: {}", userId, e.getMessage());
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyValue(ErrorResponse.of("Error al solicitar el informe"));
            });
    }

    /**
     * GET /api/v1/reports/{id}
     * Consulta el estado de un informe.
     * El frontend llama a este endpoint cada 3s hasta que status sea READY o FAILED.
     * Retorna 404 si el informe no existe, 200 con ReportResponse en cualquier otro caso.
     */
    public Mono<ServerResponse> getById(ServerRequest request) {
        UUID reportId = UUID.fromString(request.pathVariable("id"));
        return getReportUseCase.findById(reportId)
            .map(ReportResponse::from)
            .flatMap(resp -> ServerResponse.ok().bodyValue(resp))
            // GetReportUseCaseImpl lanza IllegalArgumentException cuando no existe
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.notFound().build())
            .onErrorResume(e -> {
                log.error("Error fetching report id={}: {}", reportId, e.getMessage());
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyValue(ErrorResponse.of("Error al obtener el informe"));
            });
    }

    private UUID extractUserId(ServerRequest request) {
        String header = request.headers().firstHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        return UUID.fromString(header);
    }
}
