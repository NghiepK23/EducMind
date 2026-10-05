package com.educmind.notification.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.notification.dto.NotificationPageResult;
import com.educmind.notification.dto.NotificationResponse;
import com.educmind.notification.dto.PreferenceResponse;
import com.educmind.notification.entity.Notification;
import com.educmind.notification.entity.NotificationPreference;
import com.educmind.notification.repository.NotificationPreferenceRepository;
import com.educmind.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationQueryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationQueryService(NotificationRepository notificationRepository,
                                    NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPageResult list(UUID userId, String status, Long sinceSeq, Integer limitParam) {
        List<String> statuses = parseStatuses(status);
        int limit = (limitParam == null || limitParam < 1) ? DEFAULT_LIMIT : Math.min(limitParam, MAX_LIMIT);
        PageRequest page = PageRequest.of(0, limit + 1);

        List<Notification> rows = (sinceSeq == null)
                ? notificationRepository.findLatest(userId, statuses, page)
                : notificationRepository.findSince(userId, statuses, sinceSeq, page);

        boolean hasMore = rows.size() > limit;
        List<Notification> visible = hasMore ? rows.subList(0, limit) : rows;
        long latestSeq = notificationRepository.findLatestSeq(userId);
        return new NotificationPageResult(
                visible.stream().map(NotificationResponse::from).toList(), limit, latestSeq, hasMore);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndStatus(userId, Notification.STATUS_UNREAD);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
                        "Không tìm thấy thông báo", HttpStatus.NOT_FOUND));
        if (!n.getRecipientId().equals(userId)) {
            throw new BusinessException("FORBIDDEN_NOT_RECIPIENT",
                    "Thông báo này không phải của bạn", HttpStatus.FORBIDDEN);
        }
        n.markRead();
        return NotificationResponse.from(notificationRepository.save(n));
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }

    @Transactional(readOnly = true)
    public List<PreferenceResponse> listPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(p -> new PreferenceResponse(p.getNotificationType(), p.isEnabled()))
                .toList();
    }

    @Transactional
    public PreferenceResponse setPreference(UUID userId, String type, boolean enabled) {
        NotificationPreference p = preferenceRepository.findByUserIdAndNotificationType(userId, type)
                .orElseGet(() -> new NotificationPreference(userId, type, enabled));
        p.setEnabled(enabled);
        p = preferenceRepository.save(p);
        return new PreferenceResponse(p.getNotificationType(), p.isEnabled());
    }

    private static List<String> parseStatuses(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return List.of(Notification.STATUS_UNREAD, Notification.STATUS_READ);
        }
        if ("UNREAD".equalsIgnoreCase(status)) {
            return List.of(Notification.STATUS_UNREAD);
        }
        if ("READ".equalsIgnoreCase(status)) {
            return List.of(Notification.STATUS_READ);
        }
        throw new BusinessException("INVALID_FILTER",
                "status phải là UNREAD, READ hoặc ALL", HttpStatus.BAD_REQUEST);
    }
}
