package com.reactivecommerce.report.infrastructure.entrypoint.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.report.domain.model.ReportStatus;
import com.reactivecommerce.report.domain.model.ReportType;
import com.reactivecommerce.report.domain.port.in.GetReportUseCase;
import com.reactivecommerce.report.domain.port.in.RequestReportUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Consumer reactivo del Report Service.
 *
 * Tópicos consumidos:
 *
 *   download.requested  → registro de descarga para estadísticas del creator.
 *   order.completed     → registro de venta para informes de ventas y plataforma.
 *   report.requested    → self-consume: verifica informes atascados en PENDING
 *                         y los reintenta si el proceso de generación falló.
 *
 * Todos los mensajes confirman el offset solo tras procesamiento exitoso.
 * En caso de error se loguea y se confirma igualmente (al menos una vez)
 * para evitar bloquear la partición. En producción añadir DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportKafkaConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final RequestReportUseCase          requestReportUseCase;
    private final GetReportUseCase              getReportUseCase;  // ← puerto in, no repositorio directo
    private final ObjectMapper                  objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
            .flatMap(this::process)
            .doOnError(e -> log.error("Kafka error in report-service: {}", e.getMessage()))
            .retry()
            .subscribe();
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(record.value(), Map.class))
            .flatMap(payload -> switch (record.topic()) {
                case "download.requested" -> handleDownloadRequested(payload);
                case "order.completed"    -> handleOrderCompleted(payload);
                case "report.requested"   -> handleReportRequested(payload);
                default -> {
                    log.warn("Unhandled topic: {}", record.topic());
                    yield Mono.<Void>empty();
                }
            })
            .doOnSuccess(v -> record.receiverOffset().acknowledge())
            .doOnError(e -> log.error("Error processing topic={} key={}: {}",
                record.topic(), record.key(), e.getMessage()))
            .onErrorResume(e -> {
                // Confirm offset to avoid partition blocking; add to DLQ in production
                record.receiverOffset().acknowledge();
                return Mono.empty();
            });
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    /**
     * Registra una descarga para las estadísticas del creator.
     * Estructura del payload esperado:
     *   { tokenId, assetId, buyerId, orderId, createdAt }
     */
    private Mono<Void> handleDownloadRequested(Map<String, Object> payload) {
        String assetId = payload.getOrDefault("assetId", "unknown").toString();
        String buyerId = payload.getOrDefault("buyerId", "unknown").toString();
        String tokenId = payload.getOrDefault("tokenId", "unknown").toString();
        log.info("Download stat registered: assetId={} buyerId={} tokenId={}",
            assetId, buyerId, tokenId);
        // TODO (producción): persistir en colección download_stats para HU-REP-01
        return Mono.empty();
    }

    /**
     * Registra una venta para los informes de ventas.
     * Estructura del payload esperado:
     *   { orderId, assetId, buyerId, creatorId, amount, platformCommission, createdAt }
     */
    private Mono<Void> handleOrderCompleted(Map<String, Object> payload) {
        String orderId    = payload.getOrDefault("orderId",  "unknown").toString();
        String assetId    = payload.getOrDefault("assetId",  "unknown").toString();
        String amount     = payload.getOrDefault("amount",   "0").toString();
        log.info("Sale stat registered: orderId={} assetId={} amount={}",
            orderId, assetId, amount);
        // TODO (producción): persistir en colección sale_stats para HU-REP-01 y HU-REP-02
        return Mono.empty();
    }

    /**
     * Self-consume de report.requested.
     *
     * Propósito: recovery de informes atascados en PENDING.
     * Si el pod se cayó durante la generación, el próximo arranque retoma
     * el informe al consumir este evento desde Kafka.
     *
     * Usa GetReportUseCase (puerto in) para consultar el estado actual
     * antes de decidir si relanzar la generación.
     */
    private Mono<Void> handleReportRequested(Map<String, Object> payload) {
        try {
            UUID   reportId    = UUID.fromString(payload.get("reportId").toString());
            UUID   requestedBy = UUID.fromString(payload.get("requestedBy").toString());
            String type        = payload.get("type").toString();
            String periodFrom  = payload.getOrDefault("periodFrom", "").toString();
            String periodTo    = payload.getOrDefault("periodTo",   "").toString();

            log.info("report.requested received: reportId={} type={}", reportId, type);

            return getReportUseCase.findById(reportId)
                .flatMap(existing -> {
                    // Solo relanzar si está atascado en PENDING (no en PROCESSING/READY/FAILED)
                    if (existing.status() != ReportStatus.PENDING) {
                        log.debug("Report {} already in status {}, skipping recovery",
                            reportId, existing.status());
                        return Mono.<Void>empty();
                    }
                    log.warn("Recovering stuck PENDING report: {}", reportId);
                    return requestReportUseCase.execute(
                        new RequestReportUseCase.Command(
                            requestedBy,
                            ReportType.valueOf(type),
                            periodFrom.isEmpty() ? null : Instant.parse(periodFrom),
                            periodTo.isEmpty()   ? null : Instant.parse(periodTo)
                        )
                    ).then();
                })
                .onErrorResume(e -> {
                    log.warn("Could not find report {} for recovery: {}", reportId, e.getMessage());
                    return Mono.empty();
                });
        } catch (Exception e) {
            log.error("Failed to parse report.requested payload: {}", e.getMessage());
            return Mono.empty();
        }
    }
}
