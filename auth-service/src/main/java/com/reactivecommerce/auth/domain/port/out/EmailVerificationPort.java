package com.reactivecommerce.auth.domain.port.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface EmailVerificationPort {
    Mono<String> createVerificationToken(UUID userId);
    Mono<UUID> validateToken(String token);
    Mono<Void> deleteToken(String token);
}
