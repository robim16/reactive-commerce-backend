package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.model.User;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de persistencia para User.
 *
 * SOLUCIÓN AL INSERT/UPDATE AMBIGUO:
 * Spring Data R2DBC decide INSERT vs UPDATE según si el @Id es null.
 * Como el dominio genera el UUID antes de persistir, el @Id siempre
 * llega no-nulo → Spring Data emite UPDATE → "Row does not exist".
 *
 * En lugar de Persistable<UUID> (que tiene conflictos con Lombok @Builder
 * en Spring Boot 3.3.x), se usa R2dbcEntityTemplate directamente:
 *
 *   save()   → template.insert()  — INSERT explícito, sin ambigüedad
 *   update() → template.update()  — UPDATE explícito con criterio por ID
 *
 * R2dbcEntityTemplate.insert() siempre emite INSERT independientemente
 * del valor del @Id. R2dbcEntityTemplate.update() siempre emite UPDATE.
 * No hay lógica de detección — la operación es declarativa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final R2dbcEntityTemplate  template;
    private final UserR2dbcRepository  r2dbcRepository;

    @Override
    public Mono<User> save(User user) {
        // INSERT explícito — nunca emite UPDATE aunque el ID no sea null
        return template.insert(toEntity(user))
            .map(this::toDomain)
            .doOnSuccess(u -> log.debug("User inserted: id={} email={}", u.id(), u.email()));
    }

    @Override
    public Mono<User> update(User user) {
        // UPDATE explícito por ID — solo actualiza los campos mutables
        return template.update(
                Query.query(Criteria.where("id").is(user.id())),
                Update.update("name",            user.name())
                      .set("email",              user.email())
                      .set("password_hash",      user.passwordHash())
                      .set("role",               user.role().name())
                      .set("email_verified",     user.emailVerified())
                      .set("active",             user.active())
                      .set("oauth_provider",     user.oauthProvider())
                      .set("oauth_provider_id",  user.oauthProviderId())
                      .set("updated_at",         Instant.now()),
                UserEntity.class
            )
            .flatMap(count -> {
                if (count == 0) {
                    return Mono.error(new IllegalStateException(
                        "Usuario no encontrado para actualizar: " + user.id()));
                }
                return findById(user.id());
            })
            .doOnSuccess(u -> log.debug("User updated: id={} email={}", u.id(), u.email()));
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return r2dbcRepository.findByEmail(email)
            .map(this::toDomain);
    }

    @Override
    public Mono<User> findById(UUID id) {
        return r2dbcRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return r2dbcRepository.existsByEmail(email);
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private UserEntity toEntity(User u) {
        return UserEntity.builder()
            .id(u.id())
            .name(u.name())
            .email(u.email())
            .passwordHash(u.passwordHash())
            .role(u.role())
            .emailVerified(u.emailVerified())
            .active(u.active())
            .oauthProvider(u.oauthProvider())
            .oauthProviderId(u.oauthProviderId())
            .createdAt(u.createdAt())
            .updatedAt(u.updatedAt())
            .build();
    }

    private User toDomain(UserEntity e) {
        return User.builder()
            .id(e.getId())
            .name(e.getName())
            .email(e.getEmail())
            .passwordHash(e.getPasswordHash())
            .role(e.getRole())
            .emailVerified(e.isEmailVerified())
            .active(e.isActive())
            .oauthProvider(e.getOauthProvider())
            .oauthProviderId(e.getOauthProviderId())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt())
            .build();
    }
}
