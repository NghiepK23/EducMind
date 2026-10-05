package com.educmind.notification.service;

import com.educmind.notification.entity.Notification;
import com.educmind.notification.repository.NotificationPreferenceRepository;
import com.educmind.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tạo notification hàng loạt. Tôn trọng notification_preferences (Module 10 J.5.5):
 * người đã tắt loại này thì KHÔNG tạo dòng (không tạo rồi ẩn).
 */
@Service
public class NotificationCreationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationCreationService(NotificationRepository notificationRepository,
                                       NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional
    public int createForRecipients(String type, String sourceEventType, UUID sourceEntityId,
                                   Collection<UUID> recipientIds, String title, String body,
                                   Map<String, Object> payload) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return 0;
        }
        Set<UUID> distinct = new HashSet<>(recipientIds);
        Set<UUID> disabled = new HashSet<>(preferenceRepository.findDisabledUserIds(type, distinct));
        List<Notification> toSave = distinct.stream()
                .filter(id -> !disabled.contains(id))
                .map(id -> new Notification(id, type, title, body, payload, sourceEventType, sourceEntityId))
                .toList();
        notificationRepository.saveAll(toSave);
        return toSave.size();
    }
}
