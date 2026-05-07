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
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de persistencia para Asset.
 *
 * SOLUCIÓN INSERT/UPDATE AMBIGUO:
 * Asset.create() genera UUID.randomUUID() antes de persistir.
 * Con r2dbcRepository.save() el @Id no-nulo provoca UPDATE → "Row does not exist".
 *
 * Se usa R2dbcEntityTemplate directamente:
 *   save()   → template.insert()  → INSERT explícito siempre
 *   update() → template.update()  → UPDATE explícito con criterio por ID
 *
 * Mismo patrón aplicado en UserRepositoryAdapter (auth-service).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetRepositoryAdapter implements AssetRepository {

    private final R2dbcEntityTemplate  template;
    private final AssetR2dbcRepository r2dbcRepository;
    private final ObjectMapper         objectMapper;

    @Override
    public Mono<Asset> save(Asset asset) {
        return template.insert(toEntity(asset))
            .map(this::toDomain)
            .doOnSuccess(a -> log.debug("Asset inserted: id={} title={}", a.id(), a.title()));
    }

    @Override
    public Mono<Asset> findById(UUID id) {
        return r2dbcRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<Asset> search(SearchAssetsUseCase.Query query) {
        Flux<AssetEntity> base = query.category() != null
            ? r2dbcRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                query.category(), AssetStatus.PUBLISHED)
            : r2dbcRepository.findByStatusOrderByCreatedAtDesc(AssetStatus.PUBLISHED);

        return base
            .filter(e -> query.text() == null
                || e.getTitle().toLowerCase().contains(query.text().toLowerCase()))
            .filter(e -> query.minPrice() == null
                || e.getPrice().compareTo(query.minPrice()) >= 0)
            .filter(e -> query.maxPrice() == null
                || e.getPrice().compareTo(query.maxPrice()) <= 0)
            .filter(e -> query.minRating() == null
                || e.getAverageRating() >= query.minRating())
            .take(query.pageSize())
            .map(this::toDomain);
    }

    @Override
    public Flux<Asset> findByStatus(AssetStatus status) {
        return r2dbcRepository.findByStatusOrderByCreatedAtDesc(status).map(this::toDomain);
    }

    @Override
    public Mono<Asset> update(Asset asset) {
        return template.update(
                Query.query(Criteria.where("id").is(asset.id())),
                buildUpdate(asset),
                AssetEntity.class
            )
            .flatMap(count -> {
                if (count == 0) {
                    return Mono.error(new IllegalStateException(
                        "Asset not found for update: " + asset.id()));
                }
                return findById(asset.id());
            })
            .doOnSuccess(a -> log.debug("Asset updated: id={} status={}", a.id(), a.status()));
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return template.delete(
                Query.query(Criteria.where("id").is(id)),
                AssetEntity.class
            )
            .then()
            .doOnSuccess(v -> log.debug("Asset deleted: id={}", id));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @SneakyThrows
    private Update buildUpdate(Asset a) {
        return Update
            .update("title",            a.title())
            .set("description",         a.description())
            .set("category",            a.category() != null ? a.category().name() : null)
            .set("tags_json",           objectMapper.writeValueAsString(a.tags()))
            .set("price",               a.price())
            .set("license",             a.license())
            .set("status",              a.status() != null ? a.status().name() : null)
            .set("s3_key",              a.s3Key())
            .set("thumbnail_s3_key",    a.thumbnailS3Key())
            .set("format",              a.format())
            .set("file_size_bytes",     a.fileSizeBytes())
            .set("average_rating",      a.averageRating())
            .set("total_reviews",       a.totalReviews())
            .set("total_sales",         a.totalSales())
            .set("rejection_reason",    a.rejectionReason())
            .set("moderated_by",        a.moderatedBy())
            .set("moderated_at",        a.moderatedAt())
            .set("updated_at",          Instant.now());
    }

    @SneakyThrows
    private AssetEntity toEntity(Asset a) {
        return AssetEntity.builder()
            .id(a.id())
            .title(a.title())
            .description(a.description())
            .category(a.category())
            .tagsJson(objectMapper.writeValueAsString(a.tags()))
            .price(a.price())
            .license(a.license())
            .status(a.status())
            .creatorId(a.creatorId())
            .s3Key(a.s3Key())
            .thumbnailS3Key(a.thumbnailS3Key())
            .format(a.format())
            .fileSizeBytes(a.fileSizeBytes())
            .averageRating(a.averageRating())
            .totalReviews(a.totalReviews())
            .totalSales(a.totalSales())
            .rejectionReason(a.rejectionReason())
            .moderatedBy(a.moderatedBy())
            .moderatedAt(a.moderatedAt())
            .createdAt(a.createdAt())
            .updatedAt(a.updatedAt())
            .build();
    }

    @SneakyThrows
    private Asset toDomain(AssetEntity e) {
        List<String> tags = e.getTagsJson() != null
            ? objectMapper.readValue(e.getTagsJson(), new TypeReference<>() {})
            : List.of();
        return Asset.builder()
            .id(e.getId())
            .title(e.getTitle())
            .description(e.getDescription())
            .category(e.getCategory())
            .tags(tags)
            .price(e.getPrice())
            .license(e.getLicense())
            .status(e.getStatus())
            .creatorId(e.getCreatorId())
            .s3Key(e.getS3Key())
            .thumbnailS3Key(e.getThumbnailS3Key())
            .format(e.getFormat())
            .fileSizeBytes(e.getFileSizeBytes())
            .averageRating(e.getAverageRating())
            .totalReviews(e.getTotalReviews())
            .totalSales(e.getTotalSales())
            .rejectionReason(e.getRejectionReason())
            .moderatedBy(e.getModeratedBy())
            .moderatedAt(e.getModeratedAt())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .build();
    }
}
