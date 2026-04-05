package com.reactivecommerce.report.application.usecase;

import com.reactivecommerce.report.domain.model.Report;
import com.reactivecommerce.report.domain.port.in.GetReportUseCase;
import com.reactivecommerce.report.domain.port.out.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Caso de uso de consulta de informe.
 * Invocado desde ReportHandler en GET /api/v1/reports/{id}.
 * El frontend llama a este endpoint cada 3 segundos (RTK Query pollingInterval)
 * hasta que el status sea READY o FAILED.
 */
@Service
@RequiredArgsConstructor
public class GetReportUseCaseImpl implements GetReportUseCase {

    private final ReportRepository reportRepository;

    @Override
    public Mono<Report> findById(UUID reportId) {
        return reportRepository.findById(reportId)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Report not found: " + reportId)));
    }
}
