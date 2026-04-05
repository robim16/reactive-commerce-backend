package com.reactivecommerce.report.domain.port.in;

import com.reactivecommerce.report.domain.model.Report;
import com.reactivecommerce.report.domain.model.ReportType;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

public interface RequestReportUseCase {
    record Command(UUID requestedBy, ReportType type, Instant periodFrom, Instant periodTo) {}
    Mono<Report> execute(Command command);
}
