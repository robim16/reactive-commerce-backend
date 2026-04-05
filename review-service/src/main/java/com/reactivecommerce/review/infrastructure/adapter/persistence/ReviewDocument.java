package com.reactivecommerce.review.infrastructure.adapter.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Documento MongoDB para Review.
 *
 * Colección: reviews
 *
 * Índices:
 *   assetId + status  → consultas de reseñas visibles por asset (compound, más selectivo)
 *   buyerId           → historial de reseñas del buyer
 *   assetId + buyerId → unique constraint de una reseña por buyer/asset
 *
 * No se usa TTL: las reseñas son permanentes para el historial del creator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
@CompoundIndex(name = "asset_buyer_idx", def = "{'assetId': 1, 'buyerId': 1}", unique = true)
@CompoundIndex(name = "asset_status_idx", def = "{'assetId': 1, 'status': 1}")
public class ReviewDocument {

    @Id
    private String id;

    @Indexed
    private String assetId;

    @Indexed
    private String buyerId;

    private int    rating;
    private String comment;
    private String status;      // ReviewStatus.name()
    private String hideReason;

    private Instant createdAt;
    private Instant updatedAt;
}
