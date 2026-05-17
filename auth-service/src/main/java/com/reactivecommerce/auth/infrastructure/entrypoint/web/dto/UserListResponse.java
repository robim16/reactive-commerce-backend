package com.reactivecommerce.auth.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.auth.domain.model.User;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para el listado de usuarios.
 * Nunca expone passwordHash ni oauthProviderId.
 */
public record UserListResponse(
    UUID    id,
    String  name,
    String  email,
    String  role,
    boolean emailVerified,
    boolean active,
    Instant createdAt
) {
    public static UserListResponse from(User u) {
        return new UserListResponse(
            u.id(),
            u.name(),
            u.email(),
            u.role() != null ? u.role().name() : null,
            u.emailVerified(),
            u.active(),
            u.createdAt()
        );
    }
}
