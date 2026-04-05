package com.reactivecommerce.auth.domain.model;

import lombok.Builder;
import lombok.With;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
public record User(
    UUID id,
    String name,
    String email,
    String passwordHash,
    UserRole role,
    boolean emailVerified,
    boolean active,
    String oauthProvider,
    String oauthProviderId,
    Instant createdAt,
    Instant updatedAt
) {
    public static User create(String name, String email, String passwordHash, UserRole role) {
        return User.builder()
            .id(UUID.randomUUID())
            .name(name)
            .email(email)
            .passwordHash(passwordHash)
            .role(role)
            .emailVerified(false)
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    public User verify() {
        return this.withEmailVerified(true).withUpdatedAt(Instant.now());
    }
}
