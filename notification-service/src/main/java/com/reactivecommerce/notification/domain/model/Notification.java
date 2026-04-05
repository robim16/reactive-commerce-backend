package com.reactivecommerce.notification.domain.model;

import lombok.Builder;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
@Document("notifications")
public record Notification(
    @Id String id,
    UUID userId,
    NotificationType type,
    String title,
    String message,
    boolean read,
    Instant createdAt
) {
    public static Notification create(UUID userId, NotificationType type, String title, String message) {
        return Notification.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId).type(type).title(title).message(message)
            .read(false).createdAt(Instant.now())
            .build();
    }
}
