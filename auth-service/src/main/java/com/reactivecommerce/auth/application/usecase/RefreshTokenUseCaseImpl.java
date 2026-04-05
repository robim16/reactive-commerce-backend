package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.model.TokenPair;
import com.reactivecommerce.auth.domain.port.in.RefreshTokenUseCase;
import com.reactivecommerce.auth.domain.port.out.TokenBlacklistPort;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import com.reactivecommerce.auth.infrastructure.config.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenBlacklistPort tokenBlacklistPort;

    @Override
    public Mono<TokenPair> execute(String refreshToken) {
        return tokenBlacklistPort.isBlacklisted(refreshToken)
            .flatMap(blacklisted -> {
                if (blacklisted) {
                    return Mono.error(new IllegalArgumentException("Token revoked"));
                }
                UUID userId = jwtService.extractUserId(refreshToken);
                return userRepository.findById(userId);
            })
            .map(jwtService::generateTokenPair);
    }
}
