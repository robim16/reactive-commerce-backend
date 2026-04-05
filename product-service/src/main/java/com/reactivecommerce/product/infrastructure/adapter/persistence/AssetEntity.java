package com.reactivecommerce.product.infrastructure.adapter.persistence;

import com.reactivecommerce.product.domain.model.AssetCategory;
import com.reactivecommerce.product.domain.model.AssetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("assets")
public class AssetEntity {
    @Id
    private UUID id;
    private String title;
    private String description;
    private AssetCategory category;
    private String tagsJson;
    private BigDecimal price;
    private String license;
    private AssetStatus status;
    private UUID creatorId;
    private String s3Key;
    private String thumbnailS3Key;
    private String format;
    private Long fileSizeBytes;
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalSales;
    private String rejectionReason;
    private UUID moderatedBy;
    private Instant moderatedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
