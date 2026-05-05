package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.model.User;
import com.reactivecommerce.auth.domain.port.in.RegisterUserUseCase;
import com.reactivecommerce.auth.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.auth.domain.port.out.EmailVerificationPort;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Implementación del caso de uso de registro.
 *
 * Dependencias — todas son puertos del dominio o de Spring Security
 * (PasswordEncoder es una abstracción, no una implementación concreta):
 *
 *   UserRepository        → puerto out de persistencia
 *   DomainEventPublisher  → puerto out de mensajería
 *   EmailVerificationPort → puerto out de verificación de email
 *   PasswordEncoder       → abstracción de Spring Security (no infraestructura propia)
 *
 * Ningún use case de esta capa importa clases de infraestructura.
 *
 * Flujo:
 *  1. Verificar email único.
 *  2. Crear usuario con emailVerified=false.
 *  3. Persistir.
 *  4. Generar token de verificación en Redis (via EmailVerificationPort).
 *  5. Publicar user.registered con el verificationToken para que
 *     Notification Service envíe el email con el enlace de verificación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository        userRepository;
    private final DomainEventPublisher  eventPublisher;
    private final PasswordEncoder       passwordEncoder;
    private final EmailVerificationPort emailVerificationPort;

    @Override
    public Mono<User> execute(Command command) {
        if (command.name()     == null || command.name().isBlank())     return Mono.error(new IllegalArgumentException("El nombre es obligatorio"));
        if (command.email()    == null || command.email().isBlank())    return Mono.error(new IllegalArgumentException("El email es obligatorio"));
        if (command.password() == null || command.password().isBlank()) return Mono.error(new IllegalArgumentException("La contraseña es obligatoria"));
        if (command.role()     == null)                                 return Mono.error(new IllegalArgumentException("El rol es obligatorio"));

        return userRepository.existsByEmail(command.email())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalStateException(
                        "El email ya está registrado: " + command.email()));
                }
                String passwordHash = passwordEncoder.encode(command.password());
                User user = User.create(command.name(), command.email(), passwordHash, command.role());
                return userRepository.save(user);
            })
            .flatMap(saved ->
                emailVerificationPort.createVerificationToken(saved.id())
                    .flatMap(verificationToken ->
                        eventPublisher.publish(
                            "user.registered",
                            saved.id().toString(),
                            Map.of(
                                "userId",            saved.id(),
                                "email",             saved.email(),
                                "name",              saved.name(),
                                "verificationToken", verificationToken
                            )
                        ).thenReturn(saved)
                    )
            )
            .doOnSuccess(u -> log.info("Usuario registrado: {}", u.email()));
    }
}
