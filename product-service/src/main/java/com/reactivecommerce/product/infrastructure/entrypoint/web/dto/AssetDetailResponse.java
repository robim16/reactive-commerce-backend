package com.reactivecommerce.product.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.product.domain.model.Asset;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssetDetailResponse(UUID id, String title, String description, String category,
                                   List<String> tags, BigDecimal price, String license, String status,
                                   UUID creatorId, Double averageRating, Integer totalReviews,
                                   Integer totalSales) {
    public static AssetDetailResponse from(Asset a) {
        return new AssetDetailResponse(a.id(), a.title(), a.description(), a.category().name(),
            a.tags(), a.price(), a.license(), a.status().name(), a.creatorId(),
            a.averageRating(), a.totalReviews(), a.totalSales());
    }
}
