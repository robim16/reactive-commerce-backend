package com.reactivecommerce.report.infrastructure.adapter.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Documento MongoDB para Report.
 *
 * Colección: reports
 * Índices:
 *   requestedBy  → consultas del panel del creator/admin
 *   status       → filtrado por PENDING/PROCESSING para el job de limpieza
 *   createdAt    → TTL: informes eliminados después de 90 días
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
public class ReportDocument {

    @Id
    private String id;

    @Indexed
    private String requestedBy;

    private String type;

    @Indexed
    private String status;

    private String period;
    private Instant periodFrom;
    private Instant periodTo;
    private String s3Key;
    private String presignedUrl;
    private int progressPercent;

    @Indexed(expireAfterSeconds = 60 * 60 * 24 * 90) // TTL: 90 días
    private Instant createdAt;

    private Instant completedAt;
}
