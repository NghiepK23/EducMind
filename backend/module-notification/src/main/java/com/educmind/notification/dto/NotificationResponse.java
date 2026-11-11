package com.educmind.notification.dto;

import com.educmind.notification.entity.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String content,
                                   String relatedEntityType, UUID relatedEntityId,
                                   boolean isRead, OffsetDateTime createdAt, OffsetDateTime readAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getTitle(), n.getContent(),
                n.getRelatedEntityType(), n.getRelatedEntityId(), n.isRead(),
                n.getCreatedAt(), n.getReadAt());
    }
}
