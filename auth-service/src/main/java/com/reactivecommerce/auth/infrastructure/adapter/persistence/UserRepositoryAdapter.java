package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.model.User;
import com.reactivecommerce.auth.domain.model.UserRole;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserR2dbcRepository r2dbcRepository;

    @Override
    public Mono<User> save(User user) {
        return r2dbcRepository.save(toEntity(user)).map(this::toDomain);
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
        return r2dbcRepository.save(toEntity(user)).map(this::toDomain);
    }

    private UserEntity toEntity(User u) {
        return UserEntity.builder()
            .id(u.id()).name(u.name()).email(u.email())
            .passwordHash(u.passwordHash()).role(u.role())
            .emailVerified(u.emailVerified()).active(u.active())
            .oauthProvider(u.oauthProvider()).oauthProviderId(u.oauthProviderId())
            .createdAt(u.createdAt()).updatedAt(u.updatedAt())
            .build();
    }

    private User toDomain(UserEntity e) {
        return User.builder()
            .id(e.getId()).name(e.getName()).email(e.getEmail())
            .passwordHash(e.getPasswordHash()).role(e.getRole())
            .emailVerified(e.isEmailVerified()).active(e.isActive())
            .oauthProvider(e.getOauthProvider()).oauthProviderId(e.getOauthProviderId())
            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
            .build();
    }
}
