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

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;
    private final EmailVerificationPort emailVerificationPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<User> execute(Command command) {
        return userRepository.existsByEmail(command.email())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalStateException("Email already registered: " + command.email()));
                }
                String passwordHash = passwordEncoder.encode(command.password());
                User user = User.create(command.name(), command.email(), passwordHash, command.role());
                return userRepository.save(user);
            })
            .flatMap(saved -> emailVerificationPort.createVerificationToken(saved.id())
                .flatMap(verificationToken -> eventPublisher.publish(
                    "user.registered",
                    saved.id().toString(),
                    Map.of(
                        "userId",            saved.id(),
                        "email",             saved.email(),
                        "name",              saved.name(),
                        "verificationToken", verificationToken  // Notification Service lo incluye en el email
                    )
                ))
                .thenReturn(saved)
            )
            .doOnSuccess(u -> log.info("User registered: {}", u.email()));
    }
}
