package com.educmind.notification.dto;

import com.educmind.notification.entity.Notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String body,
                                   Map<String, Object> payload, String status,
                                   OffsetDateTime readAt, long sequenceNumber,
                                   OffsetDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getPayload(), n.getStatus(), n.getReadAt(),
                n.getSequenceNumber() == null ? 0L : n.getSequenceNumber(), n.getCreatedAt());
    }
}
