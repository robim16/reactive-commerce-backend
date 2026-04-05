package com.reactivecommerce.auth.domain.port.in;

import reactor.core.publisher.Mono;

public interface VerifyEmailUseCase {
    Mono<Void> execute(String token);
}
