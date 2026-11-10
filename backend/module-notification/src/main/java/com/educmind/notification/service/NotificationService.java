package com.educmind.notification.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.pagination.PaginationSupport;
import com.educmind.notification.dto.NotificationResponse;
import com.educmind.notification.entity.Notification;
import com.educmind.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * US-G03 — đọc thông báo của chính mình. AC-G03.1 (sinh tự động) do trigger DB đảm nhiệm,
 * AC-G03.2 (cập nhật trạng thái đã xem) do service này, AC-G03.3 (chỉ xem của mình) được
 * bảo đảm bằng điều kiện userId trong mọi truy vấn.
 */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public record NotificationPage(List<NotificationResponse> items, Map<String, Object> meta) {
    }

    @Transactional(readOnly = true)
    public NotificationPage list(UUID userId, Integer pageParam, Integer sizeParam) {
        int page = PaginationSupport.normalizePage(pageParam);
        int size = PaginationSupport.normalizeSize(sizeParam);

        Page<Notification> result = repository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, size));

        List<NotificationResponse> items = result.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return new NotificationPage(items,
                PaginationSupport.meta(page, size, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
                        "Không tìm thấy thông báo", HttpStatus.NOT_FOUND));
        notification.markRead();
        return NotificationResponse.from(repository.save(notification));
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId, OffsetDateTime.now());
    }
}
