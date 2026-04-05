package com.reactivecommerce.download.domain.port.in;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface RevokeTokenUseCase {
    Mono<Void> execute(UUID orderId);
}
