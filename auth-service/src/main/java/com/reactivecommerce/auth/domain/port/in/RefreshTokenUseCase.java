package com.reactivecommerce.auth.domain.port.in;

import com.reactivecommerce.auth.domain.model.TokenPair;
import reactor.core.publisher.Mono;

public interface RefreshTokenUseCase {
    Mono<TokenPair> execute(String refreshToken);
}
