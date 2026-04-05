package com.reactivecommerce.auth.domain.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

public interface TokenBlacklistPort {
    Mono<Void> blacklist(String token, Duration ttl);
    Mono<Boolean> isBlacklisted(String token);
}
