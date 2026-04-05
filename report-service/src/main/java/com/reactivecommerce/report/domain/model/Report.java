package com.reactivecommerce.report.domain.model;

import lombok.Builder;
import lombok.With;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
public record Report(
    UUID id,
    UUID requestedBy,
    ReportType type,
    ReportStatus status,
    String period,
    Instant periodFrom,
    Instant periodTo,
    String s3Key,
    String presignedUrl,
    int progressPercent,
    Instant createdAt,
    Instant completedAt
) {
    public static Report create(UUID requestedBy, ReportType type,
                                 Instant periodFrom, Instant periodTo) {
        return Report.builder()
            .id(UUID.randomUUID())
            .requestedBy(requestedBy).type(type).status(ReportStatus.PENDING)
            .periodFrom(periodFrom).periodTo(periodTo)
            .progressPercent(0)
            .createdAt(Instant.now())
            .build();
    }
}
