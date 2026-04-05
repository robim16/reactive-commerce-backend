package com.reactivecommerce.report.infrastructure.adapter.persistence;

import com.reactivecommerce.report.domain.model.Report;
import com.reactivecommerce.report.domain.model.ReportStatus;
import com.reactivecommerce.report.domain.model.ReportType;
import com.reactivecommerce.report.domain.port.out.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adaptador MongoDB que implementa ReportRepository.
 * Convierte entre Report (dominio) y ReportDocument (MongoDB).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportMongoRepository mongoRepository;

    @Override
    public Mono<Report> save(Report report) {
        return mongoRepository.save(toDocument(report))
            .map(this::toDomain)
            .doOnSuccess(r -> log.debug("Report saved: id={} status={}", r.id(), r.status()));
    }

    @Override
    public Mono<Report> findById(UUID id) {
        return mongoRepository.findById(id.toString())
            .map(this::toDomain);
    }

    @Override
    public Mono<Report> update(Report report) {
        return mongoRepository.save(toDocument(report))
            .map(this::toDomain)
            .doOnSuccess(r -> log.debug("Report updated: id={} status={} progress={}%",
                r.id(), r.status(), r.progressPercent()));
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private ReportDocument toDocument(Report r) {
        return ReportDocument.builder()
            .id(r.id().toString())
            .requestedBy(r.requestedBy().toString())
            .type(r.type().name())
            .status(r.status().name())
            .period(r.period())
            .periodFrom(r.periodFrom())
            .periodTo(r.periodTo())
            .s3Key(r.s3Key())
            .presignedUrl(r.presignedUrl())
            .progressPercent(r.progressPercent())
            .createdAt(r.createdAt())
            .completedAt(r.completedAt())
            .build();
    }

    private Report toDomain(ReportDocument d) {
        return Report.builder()
            .id(UUID.fromString(d.getId()))
            .requestedBy(UUID.fromString(d.getRequestedBy()))
            .type(ReportType.valueOf(d.getType()))
            .status(ReportStatus.valueOf(d.getStatus()))
            .period(d.getPeriod())
            .periodFrom(d.getPeriodFrom())
            .periodTo(d.getPeriodTo())
            .s3Key(d.getS3Key())
            .presignedUrl(d.getPresignedUrl())
            .progressPercent(d.getProgressPercent())
            .createdAt(d.getCreatedAt())
            .completedAt(d.getCompletedAt())
            .build();
    }
}
