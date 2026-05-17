package com.reactivecommerce.auth.application.usecase;

import com.reactivecommerce.auth.domain.model.User;
import com.reactivecommerce.auth.domain.port.in.ListUsersUseCase;
import com.reactivecommerce.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListUsersUseCaseImpl implements ListUsersUseCase {

    private final UserRepository userRepository;

    @Override
    public Flux<User> execute() {
        return userRepository.findAll()
            .doOnComplete(() -> log.debug("Users listed by admin"));
    }
}
