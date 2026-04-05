package com.reactivecommerce.report.application.usecase;

import com.reactivecommerce.report.domain.model.Report;
import com.reactivecommerce.report.domain.model.ReportStatus;
import com.reactivecommerce.report.domain.port.in.RequestReportUseCase;
import com.reactivecommerce.report.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.report.domain.port.out.ReportGeneratorPort;
import com.reactivecommerce.report.domain.port.out.ReportRepository;
import com.reactivecommerce.report.infrastructure.adapter.aws.LambdaReportGeneratorAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Map;

/**
 * Implementación del caso de uso de solicitud de informe.
 *
 * Patrón fire-and-forget con Schedulers.boundedElastic:
 *  1. Persiste el Report con status PENDING → retorna inmediatamente al caller.
 *  2. En background (boundedElastic, no bloquea el event loop):
 *     a. Actualiza a PROCESSING (progress 10%).
 *     b. Invoca Lambda vía ReportGeneratorPort para generar el PDF.
 *     c. Genera presigned URL de descarga (TTL 24h).
 *     d. Actualiza a READY con s3Key, presignedUrl y progress 100%.
 *     e. Publica report.ready → Notification Service alerta al usuario.
 *     f. Si falla cualquier paso → actualiza a FAILED.
 *
 * El frontend usa RTK Query pollingInterval (3s) sobre GET /reports/{id}
 * para saber cuándo el informe está listo (GetReportUseCaseImpl).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestReportUseCaseImpl implements RequestReportUseCase {

    private final ReportRepository      reportRepository;
    private final ReportGeneratorPort   reportGeneratorPort;
    private final DomainEventPublisher  eventPublisher;

    @Override
    public Mono<Report> execute(Command command) {
        Report report = Report.create(
            command.requestedBy(), command.type(),
            command.periodFrom(), command.periodTo());

        return reportRepository.save(report)
            .flatMap(saved -> {
                // Publicar report.requested para trazabilidad/auditoría
                eventPublisher.publish(
                    "report.requested",
                    saved.id().toString(),
                    Map.of(
                        "reportId",    saved.id(),
                        "requestedBy", saved.requestedBy(),
                        "type",        saved.type().name(),
                        "periodFrom",  saved.periodFrom() != null ? saved.periodFrom().toString() : "",
                        "periodTo",    saved.periodTo()   != null ? saved.periodTo().toString()   : ""
                    )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

                // Generación asíncrona: fire-and-forget
                generateAsync(saved);

                return Mono.just(saved);
            });
    }

    // ── Generación asíncrona ───────────────────────────────────────────────

    private void generateAsync(Report saved) {
        reportRepository.update(saved.withStatus(ReportStatus.PROCESSING).withProgressPercent(10))
            .flatMap(processing -> reportGeneratorPort.generatePdf(processing))
            .flatMap(s3Key -> {
                // Generar presigned URL si el adaptador lo soporta
                if (reportGeneratorPort instanceof LambdaReportGeneratorAdapter lambda) {
                    return lambda.generatePresignedDownloadUrl(s3Key)
                        .map(presignedUrl -> Map.entry(s3Key, presignedUrl));
                }
                return Mono.just(Map.entry(s3Key, s3Key)); // fallback: s3Key como URL
            })
            .flatMap(entry -> reportRepository.update(
                saved.withStatus(ReportStatus.READY)
                    .withS3Key(entry.getKey())
                    .withPresignedUrl(entry.getValue())
                    .withProgressPercent(100)
                    .withCompletedAt(Instant.now())
            ))
            .flatMap(ready -> eventPublisher.publish(
                "report.ready",
                ready.id().toString(),
                Map.of(
                    "reportId",     ready.id(),
                    "requestedBy",  ready.requestedBy(),
                    "type",         ready.type().name(),
                    "presignedUrl", ready.presignedUrl() != null ? ready.presignedUrl() : ""
                )
            ))
            .onErrorResume(e -> {
                log.error("Report generation failed for reportId={}: {}", saved.id(), e.getMessage());
                return reportRepository.update(saved.withStatus(ReportStatus.FAILED))
                    .then(Mono.empty());
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                v  -> log.info("Report {} generated successfully", saved.id()),
                e  -> log.error("Unhandled error in report generation: {}", e.getMessage())
            );
    }
}
