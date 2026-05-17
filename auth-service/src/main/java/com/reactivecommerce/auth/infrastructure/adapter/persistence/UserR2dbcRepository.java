package com.reactivecommerce.auth.infrastructure.adapter.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, UUID> {
    Mono<UserEntity>  findByEmail(String email);
    Mono<Boolean>     existsByEmail(String email);
    Flux<UserEntity>  findAllByOrderByCreatedAtDesc();
}
