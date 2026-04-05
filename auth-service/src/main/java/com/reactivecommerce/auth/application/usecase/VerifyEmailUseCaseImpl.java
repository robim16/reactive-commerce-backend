package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.port.in.VerifyEmailUseCase;
import com.reactivecommerce.auth.domain.port.out.EmailVerificationPort;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementación del caso de uso de verificación de email.
 *
 * Flujo:
 *  1. Valida el token en Redis → obtiene el userId asociado.
 *  2. Carga el User del repositorio.
 *  3. Marca el email como verificado (User.verify()).
 *  4. Persiste el cambio.
 *  5. Elimina el token consumido para que no pueda reutilizarse.
 *
 * El token es de un solo uso: se borra tanto si la verificación es exitosa
 * como si el usuario ya estaba verificado (idempotente).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyEmailUseCaseImpl implements VerifyEmailUseCase {

    private final EmailVerificationPort emailVerificationPort;
    private final UserRepository userRepository;

    @Override
    public Mono<Void> execute(String token) {
        return emailVerificationPort.validateToken(token)
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Token de verificación inválido o expirado")))
            .flatMap(userId -> userRepository.findById(userId)
                .switchIfEmpty(Mono.error(
                    new IllegalArgumentException("Usuario no encontrado para el token proporcionado")))
                .flatMap(user -> {
                    if (user.emailVerified()) {
                        // Idempotente: ya verificado, sólo limpiamos el token
                        log.info("Email ya verificado para userId={}", userId);
                        return emailVerificationPort.deleteToken(token);
                    }
                    return userRepository.update(user.verify())
                        .flatMap(updated -> emailVerificationPort.deleteToken(token))
                        .doOnSuccess(v -> log.info("Email verificado exitosamente para userId={}", userId));
                })
            );
    }
}
