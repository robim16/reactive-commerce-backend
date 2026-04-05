package com.reactivecommerce.report.infrastructure.adapter.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.report.domain.model.Report;
import com.reactivecommerce.report.domain.port.out.ReportGeneratorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Map;

/**
 * Adaptador que implementa ReportGeneratorPort delegando en AWS Lambda.
 *
 * Flujo para generatePdf() y generateCsv():
 *  1. Serializa el Report a JSON y lo envía como payload a la función Lambda.
 *  2. Lambda genera el PDF/CSV, lo sube a S3 y devuelve {"s3Key": "reports/..."}.
 *  3. Este adaptador genera una presigned GET URL (TTL 24h) para que el usuario
 *     pueda descargar el informe directamente desde S3 sin pasar por el backend.
 *  4. Retorna la s3Key; el use case la persiste en el Report y la incluye
 *     en el evento report.ready para que Notification Service la reenvíe.
 *
 * La invocación se ejecuta en Schedulers.boundedElastic() (gestionado desde
 * RequestReportUseCaseImpl) para no bloquear el event loop de Netty.
 *
 * En entorno local/CI activar el perfil "mock-lambda" que sustituye este bean
 * por MockReportGeneratorAdapter sin llamadas reales a AWS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LambdaReportGeneratorAdapter implements ReportGeneratorPort {

    private final LambdaAsyncClient lambdaClient;
    private final S3Presigner       s3Presigner;
    private final ObjectMapper      objectMapper;

    @Value("${aws.lambda.report-function:report-generator}")
    private String reportFunction;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final Duration PRESIGNED_URL_TTL = Duration.ofHours(24);

    @Override
    public Mono<String> generatePdf(Report report) {
        return invokeLambda(report, "PDF")
            .doOnSuccess(key -> log.info("PDF generated: reportId={} s3Key={}", report.id(), key));
    }

    @Override
    public Mono<String> generateCsv(Report report) {
        return invokeLambda(report, "CSV")
            .doOnSuccess(key -> log.info("CSV generated: reportId={} s3Key={}", report.id(), key));
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Mono<String> invokeLambda(Report report, String format) {
        return Mono.fromCallable(() -> buildPayload(report, format))
            .flatMap(payload -> Mono.fromFuture(lambdaClient.invoke(
                InvokeRequest.builder()
                    .functionName(reportFunction)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build()
            )))
            .flatMap(response -> {
                if (response.functionError() != null) {
                    log.error("Lambda function error: {} — {}",
                        response.functionError(), response.payload().asUtf8String());
                    return Mono.error(new RuntimeException(
                        "Lambda error: " + response.functionError()));
                }
                // Lambda devuelve {"s3Key": "reports/{reportId}/{format}.pdf"}
                return Mono.fromCallable(() -> {
                    Map<?, ?> result = objectMapper.readValue(
                        response.payload().asUtf8String(), Map.class);
                    if (!result.containsKey("s3Key")) {
                        throw new RuntimeException("Lambda response missing s3Key field");
                    }
                    return result.get("s3Key").toString();
                });
            });
    }

    /**
     * Genera una presigned GET URL válida 24h para descarga directa del informe.
     * Llamado por el use case después de obtener la s3Key de Lambda.
     * Se expone como método público para que sea accesible si se necesita
     * regenerar la URL sin reinvocar Lambda (ej. URL expirada).
     */
    public Mono<String> generatePresignedDownloadUrl(String s3Key) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .getObjectRequest(getRequest)
                .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        }).doOnSuccess(url -> log.debug("Presigned URL generated for s3Key={}", s3Key));
    }

    private String buildPayload(Report report, String format) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "reportId",    report.id().toString(),
            "requestedBy", report.requestedBy().toString(),
            "type",        report.type().name(),
            "format",      format,
            "periodFrom",  report.periodFrom() != null ? report.periodFrom().toString() : "",
            "periodTo",    report.periodTo()   != null ? report.periodTo().toString()   : "",
            "bucket",      bucket
        ));
    }
}
