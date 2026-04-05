package com.reactivecommerce.auth.domain.port.in;

import com.reactivecommerce.auth.domain.model.AuthCredentials;
import com.reactivecommerce.auth.domain.model.TokenPair;
import reactor.core.publisher.Mono;

public interface LoginUseCase {
    Mono<TokenPair> execute(AuthCredentials credentials);
}
