package com.reactivecommerce.auth.domain.port.out;

import reactor.core.publisher.Mono;

public interface LoginAttemptPort {
    Mono<Integer> incrementFailedAttempts(String email);
    Mono<Void> resetAttempts(String email);
    Mono<Boolean> isLocked(String email);
}
