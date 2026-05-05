package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad R2DBC para la tabla users.
 *
 * Se eliminó la implementación de Persistable<UUID> porque Lombok @Builder
 * y @Data en Spring Boot 3.3.x generan conflictos con el método isNew():
 *   - @Data genera equals/hashCode incluyendo el campo isNew
 *   - @Builder inicializa isNew=false por defecto incluso para entidades nuevas
 *   - Spring Data R2DBC 3.3 no garantiza el callback de Persistable
 *     cuando se usa junto con @Builder
 *
 * La distinción INSERT vs UPDATE se delega a UserRepositoryAdapter que
 * usa R2dbcEntityTemplate con insert() y update() explícitos en lugar
 * de save() — eliminando la ambigüedad completamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity {

    @Id
    private UUID id;

    private String   name;
    private String   email;
    private String   passwordHash;
    private UserRole role;
    private boolean  emailVerified;
    private boolean  active;
    private String   oauthProvider;
    private String   oauthProviderId;
    private Instant  createdAt;
    private Instant  updatedAt;
}
