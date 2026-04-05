package com.reactivecommerce.report.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.report.domain.model.Report;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para Report.
 * No expone el modelo de dominio directamente al cliente HTTP.
 */
public record ReportResponse(
    UUID    id,
    UUID    requestedBy,
    String  type,
    String  status,
    int     progressPercent,
    String  s3Key,
    String  presignedUrl,
    Instant periodFrom,
    Instant periodTo,
    Instant createdAt,
    Instant completedAt
) {
    public static ReportResponse from(Report r) {
        return new ReportResponse(
            r.id(),
            r.requestedBy(),
            r.type().name(),
            r.status().name(),
            r.progressPercent(),
            r.s3Key(),
            r.presignedUrl(),
            r.periodFrom(),
            r.periodTo(),
            r.createdAt(),
            r.completedAt()
        );
    }
}
