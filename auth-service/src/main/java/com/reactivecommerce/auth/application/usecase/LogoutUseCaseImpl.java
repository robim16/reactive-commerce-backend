package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.port.in.LogoutUseCase;
import com.reactivecommerce.auth.domain.port.out.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final TokenBlacklistPort tokenBlacklistPort;

    @Override
    public Mono<Void> execute(String refreshToken) {
        return tokenBlacklistPort.blacklist(refreshToken, Duration.ofDays(7));
    }
}
