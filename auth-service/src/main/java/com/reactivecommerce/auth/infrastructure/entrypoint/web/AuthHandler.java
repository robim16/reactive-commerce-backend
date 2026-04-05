package com.reactivecommerce.auth.infrastructure.entrypoint.web;

import com.reactivecommerce.auth.domain.model.AuthCredentials;
import com.reactivecommerce.auth.domain.model.UserRole;
import com.reactivecommerce.auth.domain.port.in.*;
import com.reactivecommerce.auth.infrastructure.entrypoint.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler reactivo del Auth Service.
 * Recibe la petición HTTP, delega al use case correspondiente y construye la respuesta.
 * Sin ninguna lógica de negocio: esa responsabilidad pertenece al dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
            .flatMap(body -> registerUserUseCase.execute(
                new RegisterUserUseCase.Command(
                    body.name(), body.email(), body.password(),
                    UserRole.valueOf(body.role())
                )
            ))
            .flatMap(user -> ServerResponse.status(HttpStatus.CREATED)
                .bodyValue(new UserResponse(user.id(), user.name(), user.email(), user.role())))
            .onErrorResume(IllegalStateException.class, e ->
                ServerResponse.status(HttpStatus.CONFLICT).bodyValue(ErrorResponse.of(e.getMessage())))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
            .flatMap(body -> loginUseCase.execute(
                new AuthCredentials(body.email(), body.password())
            ))
            .flatMap(tokens -> ServerResponse.ok().bodyValue(
                new TokenResponse(tokens.accessToken(), tokens.refreshToken())))
            .onErrorResume(IllegalStateException.class, e ->
                ServerResponse.status(HttpStatus.LOCKED).bodyValue(ErrorResponse.of(e.getMessage())))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> refresh(ServerRequest request) {
        return request.bodyToMono(RefreshRequest.class)
            .flatMap(body -> refreshTokenUseCase.execute(body.refreshToken()))
            .flatMap(tokens -> ServerResponse.ok().bodyValue(
                new TokenResponse(tokens.accessToken(), tokens.refreshToken())))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> logout(ServerRequest request) {
        return request.bodyToMono(RefreshRequest.class)
            .flatMap(body -> logoutUseCase.execute(body.refreshToken()))
            .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> verifyEmail(ServerRequest request) {
        String token = request.queryParam("token")
            .orElseThrow(() -> new IllegalArgumentException("Missing token"));
        return verifyEmailUseCase.execute(token)
            .then(ServerResponse.ok().bodyValue(MessageResponse.of("Email verified successfully")))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.badRequest().bodyValue(ErrorResponse.of(e.getMessage())));
    }

    public Mono<ServerResponse> me(ServerRequest request) {
        return request.principal()
            .flatMap(principal -> ServerResponse.ok().bodyValue(principal.getName()))
            .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
    }
}
