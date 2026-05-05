package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.model.TokenPair;
import com.reactivecommerce.auth.domain.port.in.RefreshTokenUseCase;
import com.reactivecommerce.auth.domain.port.out.TokenBlacklistPort;
import com.reactivecommerce.auth.domain.port.out.TokenPort;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final TokenPort         tokenPort;        // ← puerto, no JwtService
    private final UserRepository    userRepository;
    private final TokenBlacklistPort tokenBlacklistPort;

    @Override
    public Mono<TokenPair> execute(String refreshToken) {
        return tokenBlacklistPort.isBlacklisted(refreshToken)
            .flatMap(blacklisted -> {
                if (blacklisted) {
                    return Mono.error(new IllegalArgumentException("Token revocado"));
                }
                if (!tokenPort.isValid(refreshToken)) {
                    return Mono.error(new IllegalArgumentException("Token inválido o expirado"));
                }
                // Verificar que sea de tipo refresh, no un access token reutilizado
                String type = tokenPort.extractType(refreshToken);
                if (!"refresh".equals(type)) {
                    return Mono.error(new IllegalArgumentException(
                        "Token inválido: se requiere un refresh token"));
                }
                UUID userId = tokenPort.extractUserId(refreshToken);
                return userRepository.findById(userId)
                    .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Usuario no encontrado")));
            })
            .map(tokenPort::generateTokenPair);
    }
}
