package com.reactivecommerce.report.domain.port.out;

import com.reactivecommerce.report.domain.model.Report;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ReportRepository {
    Mono<Report> save(Report report);
    Mono<Report> findById(UUID id);
    Mono<Report> update(Report report);
}
