package com.reactivecommerce.auth.domain.port.in;

import com.reactivecommerce.auth.domain.model.User;
import reactor.core.publisher.Flux;

/**
 * Caso de uso: listar todos los usuarios de la plataforma.
 * Solo accesible por ADMIN y MODERATOR — la validación de rol
 * la realiza el API Gateway (RoleFilter) antes de que el request
 * llegue a este servicio. El use case no repite esa validación.
 */
public interface ListUsersUseCase {
    Flux<User> execute();
}
