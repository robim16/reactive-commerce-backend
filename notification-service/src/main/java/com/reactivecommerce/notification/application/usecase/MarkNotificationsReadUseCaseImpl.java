package com.reactivecommerce.notification.application.usecase;

import com.reactivecommerce.notification.domain.port.in.MarkNotificationsReadUseCase;
import com.reactivecommerce.notification.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkNotificationsReadUseCaseImpl implements MarkNotificationsReadUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public Mono<Void> markOne(String notificationId) {
        return notificationRepository.markReadById(notificationId);
    }

    @Override
    public Mono<Void> markAll(UUID userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }
}
