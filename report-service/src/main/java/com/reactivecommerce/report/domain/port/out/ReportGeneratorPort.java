package com.reactivecommerce.report.domain.port.out;

import com.reactivecommerce.report.domain.model.Report;
import reactor.core.publisher.Mono;

public interface ReportGeneratorPort {
    Mono<String> generatePdf(Report report);   // returns s3Key
    Mono<String> generateCsv(Report report);   // returns s3Key
}
