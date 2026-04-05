package com.reactivecommerce.product.infrastructure.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivecommerce.product.domain.model.Asset;
import com.reactivecommerce.product.domain.model.AssetStatus;
import com.reactivecommerce.product.domain.port.in.SearchAssetsUseCase;
import com.reactivecommerce.product.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetRepositoryAdapter implements AssetRepository {

    private final AssetR2dbcRepository r2dbcRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Asset> save(Asset asset) {
        return r2dbcRepository.save(toEntity(asset)).map(this::toDomain);
    }

    @Override
    public Mono<Asset> findById(UUID id) {
        return r2dbcRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<Asset> search(SearchAssetsUseCase.Query query) {
        // Simplified: full-text search would use r2dbc + native query or a search engine
        Flux<AssetEntity> base = query.category() != null
            ? r2dbcRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                query.category(), AssetStatus.PUBLISHED)
            : r2dbcRepository.findByStatusOrderByCreatedAtDesc(AssetStatus.PUBLISHED);

        return base
            .filter(e -> query.text() == null || e.getTitle().contains(query.text()))
            .filter(e -> query.minPrice() == null || e.getPrice().compareTo(query.minPrice()) >= 0)
            .filter(e -> query.maxPrice() == null || e.getPrice().compareTo(query.maxPrice()) <= 0)
            .filter(e -> query.minRating() == null || e.getAverageRating() >= query.minRating())
            .take(query.pageSize())
            .map(this::toDomain);
    }

    @Override
    public Flux<Asset> findByStatus(AssetStatus status) {
        return r2dbcRepository.findByStatusOrderByCreatedAtDesc(status).map(this::toDomain);
    }

    @Override
    public Mono<Asset> update(Asset asset) {
        return r2dbcRepository.save(toEntity(asset)).map(this::toDomain);
    }


    @Override
    public Mono<Void> deleteById(UUID id) {
        return r2dbcRepository.deleteById(id)
            .doOnSuccess(v -> log.debug("Asset deleted from DB: id={}", id));
    }

    @SneakyThrows
    private AssetEntity toEntity(Asset a) {
        return AssetEntity.builder()
            .id(a.id()).title(a.title()).description(a.description())
            .category(a.category()).tagsJson(objectMapper.writeValueAsString(a.tags()))
            .price(a.price()).license(a.license()).status(a.status())
            .creatorId(a.creatorId()).s3Key(a.s3Key()).thumbnailS3Key(a.thumbnailS3Key())
            .format(a.format()).fileSizeBytes(a.fileSizeBytes())
            .averageRating(a.averageRating()).totalReviews(a.totalReviews()).totalSales(a.totalSales())
            .rejectionReason(a.rejectionReason()).moderatedBy(a.moderatedBy()).moderatedAt(a.moderatedAt())
            .createdAt(a.createdAt()).updatedAt(a.updatedAt())
            .build();
    }

    @SneakyThrows
    private Asset toDomain(AssetEntity e) {
        List<String> tags = e.getTagsJson() != null
            ? objectMapper.readValue(e.getTagsJson(), new TypeReference<>() {}) : List.of();
        return Asset.builder()
            .id(e.getId()).title(e.getTitle()).description(e.getDescription())
            .category(e.getCategory()).tags(tags).price(e.getPrice()).license(e.getLicense())
            .status(e.getStatus()).creatorId(e.getCreatorId()).s3Key(e.getS3Key())
            .thumbnailS3Key(e.getThumbnailS3Key()).format(e.getFormat()).fileSizeBytes(e.getFileSizeBytes())
            .averageRating(e.getAverageRating()).totalReviews(e.getTotalReviews()).totalSales(e.getTotalSales())
            .rejectionReason(e.getRejectionReason()).moderatedBy(e.getModeratedBy()).moderatedAt(e.getModeratedAt())
            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
            .build();
    }
}
