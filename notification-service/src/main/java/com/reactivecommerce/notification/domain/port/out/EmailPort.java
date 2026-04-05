package com.reactivecommerce.notification.domain.port.out;

import reactor.core.publisher.Mono;
import java.util.Map;

public interface EmailPort {
    Mono<Void> send(String to, String templateName, Map<String, Object> variables);
}
