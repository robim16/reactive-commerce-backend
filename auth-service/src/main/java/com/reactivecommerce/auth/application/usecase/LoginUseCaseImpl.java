package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.model.AuthCredentials;
import com.reactivecommerce.auth.domain.model.TokenPair;
import com.reactivecommerce.auth.domain.port.in.LoginUseCase;
import com.reactivecommerce.auth.domain.port.out.LoginAttemptPort;
import com.reactivecommerce.auth.domain.port.out.TokenPort;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository   userRepository;
    private final LoginAttemptPort loginAttemptPort;
    private final TokenPort        tokenPort;        // ← puerto, no JwtService
    private final PasswordEncoder  passwordEncoder;

    @Override
    public Mono<TokenPair> execute(AuthCredentials credentials) {
        return loginAttemptPort.isLocked(credentials.email())
            .flatMap(locked -> {
                if (locked) {
                    return Mono.error(new IllegalStateException(
                        "Cuenta bloqueada. Inténtalo de nuevo en 15 minutos."));
                }
                return userRepository.findByEmail(credentials.email());
            })
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Credenciales inválidas")))
            .flatMap(user -> {
                if (!user.active()) {
                    return Mono.error(new IllegalStateException("La cuenta está desactivada"));
                }
                if (!passwordEncoder.matches(credentials.password(), user.passwordHash())) {
                    return loginAttemptPort.incrementFailedAttempts(credentials.email())
                        .then(Mono.error(new IllegalArgumentException("Credenciales inválidas")));
                }
                return loginAttemptPort.resetAttempts(credentials.email())
                    .thenReturn(tokenPort.generateTokenPair(user));
            })
            .doOnSuccess(t -> log.info("Login exitoso: {}", credentials.email()));
    }
}
