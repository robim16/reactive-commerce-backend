package com.reactivecommerce.auth.domain.port.out;

import com.reactivecommerce.auth.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface UserRepository {
    Mono<User>  save(User user);
    Mono<User>  findByEmail(String email);
    Mono<User>  findById(UUID id);
    Mono<Boolean> existsByEmail(String email);
    Mono<User>  update(User user);
    Flux<User> findAll();
}
