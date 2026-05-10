package com.reactivecommerce.product.domain.port.in;

import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.model.AssetCategory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SearchAssetsUseCase {

    record Query(String text, AssetCategory category, BigDecimal minPrice,
                 BigDecimal maxPrice, Double minRating, String cursor,
                 int pageSize, SortBy sortBy) {}
    enum SortBy { NEWEST, BEST_RATED, MOST_SOLD, PRICE_ASC, PRICE_DESC }

    Flux<Asset> search(Query query);
    Mono<Asset> findById(UUID id);
    Flux<Asset> searchAll(Query query);
}
