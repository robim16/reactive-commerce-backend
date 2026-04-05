package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import com.reactivecommerce.auth.domain.port.out.LoginAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginAttemptAdapter implements LoginAttemptPort {

    private static final String PREFIX = "login_attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Integer> incrementFailedAttempts(String email) {
        String key = PREFIX + email;
        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    return redisTemplate.expire(key, LOCK_DURATION).thenReturn(count.intValue());
                }
                return Mono.just(count.intValue());
            });
    }

    @Override
    public Mono<Void> resetAttempts(String email) {
        return redisTemplate.delete(PREFIX + email).then();
    }

    @Override
    public Mono<Boolean> isLocked(String email) {
        return redisTemplate.opsForValue().get(PREFIX + email)
            .map(val -> Integer.parseInt(val) >= MAX_ATTEMPTS)
            .defaultIfEmpty(false);
    }
}
