package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.port.out.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TokenBlacklistAdapter implements TokenBlacklistPort {

    private static final String PREFIX = "blacklist:";
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> blacklist(String token, Duration ttl) {
        return redisTemplate.opsForValue()
            .set(PREFIX + token, "1", ttl)
            .then();
    }

    @Override
    public Mono<Boolean> isBlacklisted(String token) {
        return redisTemplate.hasKey(PREFIX + token);
    }
}
