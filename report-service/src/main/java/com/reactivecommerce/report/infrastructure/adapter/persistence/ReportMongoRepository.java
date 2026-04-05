package com.reactivecommerce.report.infrastructure.adapter.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ReportMongoRepository
        extends ReactiveMongoRepository<ReportDocument, String> {

    Flux<ReportDocument> findByRequestedByOrderByCreatedAtDesc(String requestedBy);
    Flux<ReportDocument> findByStatus(String status);
}
