package com.reactivecommerce.report.domain.port.in;

import com.reactivecommerce.report.domain.model.Report;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface GetReportUseCase {
    Mono<Report> findById(UUID reportId);
}
