package com.reactivecommerce.product.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.product.domain.model.Asset;
import java.math.BigDecimal;
import java.util.UUID;

public record AssetSummaryResponse(UUID id, String title, String category, BigDecimal price,
                                    Double averageRating, Integer totalSales, String thumbnailUrl) {
    public static AssetSummaryResponse from(Asset a) {
        return new AssetSummaryResponse(a.id(), a.title(), a.category().name(),
            a.price(), a.averageRating(), a.totalSales(), a.thumbnailS3Key());
    }
}
