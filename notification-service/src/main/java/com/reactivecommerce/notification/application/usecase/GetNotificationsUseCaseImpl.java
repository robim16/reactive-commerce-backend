package com.reactivecommerce.notification.application.usecase;

import com.reactivecommerce.notification.domain.model.Notification;
import com.reactivecommerce.notification.domain.port.in.GetNotificationsUseCase;
import com.reactivecommerce.notification.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetNotificationsUseCaseImpl implements GetNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public Flux<Notification> findUnread(UUID userId, int limit) {
        return notificationRepository.findUnreadByUserId(userId, limit);
    }
}
