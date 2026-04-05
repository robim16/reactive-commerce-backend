package com.reactivecommerce.auth.domain.port.in;

import reactor.core.publisher.Mono;

public interface LogoutUseCase {
    Mono<Void> execute(String refreshToken);
}
