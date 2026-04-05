package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.model.AuthCredentials;
import com.reactivecommerce.auth.domain.model.TokenPair;
import com.reactivecommerce.auth.domain.port.in.LoginUseCase;
import com.reactivecommerce.auth.domain.port.out.LoginAttemptPort;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import com.reactivecommerce.auth.infrastructure.config.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository userRepository;
    private final LoginAttemptPort loginAttemptPort;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<TokenPair> execute(AuthCredentials credentials) {
        return loginAttemptPort.isLocked(credentials.email())
            .flatMap(locked -> {
                if (locked) {
                    return Mono.error(new IllegalStateException("Account locked. Try again in 15 minutes."));
                }
                return userRepository.findByEmail(credentials.email());
            })
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid credentials")))
            .flatMap(user -> {
                if (!user.active()) {
                    return Mono.error(new IllegalStateException("Account is disabled"));
                }
                if (!passwordEncoder.matches(credentials.password(), user.passwordHash())) {
                    return loginAttemptPort.incrementFailedAttempts(credentials.email())
                        .then(Mono.error(new IllegalArgumentException("Invalid credentials")));
                }
                return loginAttemptPort.resetAttempts(credentials.email())
                    .thenReturn(jwtService.generateTokenPair(user));
            })
            .doOnSuccess(t -> log.info("Login successful: {}", credentials.email()));
    }
}
