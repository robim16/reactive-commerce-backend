package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.model.User;
import com.reactivecommerce.auth.domain.model.UserRole;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final R2dbcEntityTemplate  template;
    private final UserR2dbcRepository  r2dbcRepository;

    @Override
    public Mono<User> save(User user) {
        return template.insert(toEntity(user))
            .map(this::toDomain)
            .doOnSuccess(u -> log.debug("User inserted: id={}", u.id()));
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return r2dbcRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Mono<User> findById(UUID id) {
        return r2dbcRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return r2dbcRepository.existsByEmail(email);
    }

    @Override
    public Mono<User> update(User user) {
        return template.update(
                Query.query(Criteria.where("id").is(user.id())),
                Update.update("name",             user.name())
                      .set("email",               user.email())
                      .set("password_hash",        user.passwordHash())
                      .set("role",                user.role() != null ? user.role().name() : null)
                      .set("email_verified",       user.emailVerified())
                      .set("active",               user.active())
                      .set("oauth_provider",       user.oauthProvider())
                      .set("oauth_provider_id",    user.oauthProviderId())
                      .set("updated_at",           Instant.now()),
                UserEntity.class
            )
            .flatMap(count -> {
                if (count == 0) return Mono.error(
                    new IllegalStateException("User not found for update: " + user.id()));
                return findById(user.id());
            });
    }

    @Override
    public Flux<User> findAll() {
        return r2dbcRepository.findAllByOrderByCreatedAtDesc()
            .map(this::toDomain);
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private UserEntity toEntity(User u) {
        return UserEntity.builder()
            .id(u.id()).name(u.name()).email(u.email())
            .passwordHash(u.passwordHash())
            .role(u.role())
            .emailVerified(u.emailVerified()).active(u.active())
            .oauthProvider(u.oauthProvider()).oauthProviderId(u.oauthProviderId())
            .createdAt(u.createdAt()).updatedAt(u.updatedAt())
            .build();
    }

    private User toDomain(UserEntity e) {
        return User.builder()
            .id(e.getId()).name(e.getName()).email(e.getEmail())
            .passwordHash(e.getPasswordHash())
            .role(e.getRole())
            .emailVerified(e.isEmailVerified()).active(e.isActive())
            .oauthProvider(e.getOauthProvider()).oauthProviderId(e.getOauthProviderId())
            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
            .build();
    }
}
