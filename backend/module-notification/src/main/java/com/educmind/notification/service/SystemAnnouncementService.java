package com.educmind.notification.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.UserAnnouncementPort;
import com.educmind.notification.dto.AnnouncementRequest;
import com.educmind.notification.dto.AnnouncementResponse;
import com.educmind.notification.entity.NotificationType;
import com.educmind.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Broadcast thông báo hệ thống (Module 10, J.6.3).
 * scope: null (mặc định: SCHOOL_ADMIN = trường mình, SUPER_ADMIN = toàn hệ thống) | SCHOOL | CLASS.
 * Quota: tối đa N announcement / 24h / admin (mặc định 10, cấu hình
 * educmind.notification.announcement-daily-limit).
 */
@Service
public class SystemAnnouncementService {

    static final String SCOPE_SCHOOL = "SCHOOL";
    static final String SCOPE_CLASS = "CLASS";

    private final UserAnnouncementPort userAnnouncementPort;
    private final ClassAccessPort classAccessPort;
    private final NotificationCreationService notificationCreationService;
    private final NotificationRepository notificationRepository;
    private final int dailyLimit;

    public SystemAnnouncementService(
            UserAnnouncementPort userAnnouncementPort,
            ClassAccessPort classAccessPort,
            NotificationCreationService notificationCreationService,
            NotificationRepository notificationRepository,
            @Value("${educmind.notification.announcement-daily-limit:10}") int dailyLimit) {
        this.userAnnouncementPort = userAnnouncementPort;
        this.classAccessPort = classAccessPort;
        this.notificationCreationService = notificationCreationService;
        this.notificationRepository = notificationRepository;
        this.dailyLimit = dailyLimit;
    }

    @Transactional
    public AnnouncementResponse announce(UUID actorId, boolean superAdmin, AnnouncementRequest request) {
        String scope = normalizeScope(request.getScope());

        UUID actorSchoolId = null;
        if (!superAdmin) {
            actorSchoolId = userAnnouncementPort.findSchoolIdByUserId(actorId)
                    .orElseThrow(() -> new BusinessException("SCHOOL_SCOPE_REQUIRED",
                            "School admin phải thuộc một trường để gửi thông báo", HttpStatus.FORBIDDEN));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        List<UUID> recipients;
        String audience;

        if (SCOPE_CLASS.equals(scope)) {
            UUID classId = request.getClassId();
            if (classId == null) {
                throw invalidScope("scope=CLASS yêu cầu classId");
            }
            UUID classSchoolId = classAccessPort.findSchoolIdOfActiveClass(classId)
                    .orElseThrow(() -> new BusinessException("CLASS_NOT_FOUND",
                            "Lớp học không tồn tại hoặc đã bị xóa", HttpStatus.NOT_FOUND));
            if (!superAdmin && !classSchoolId.equals(actorSchoolId)) {
                throw new BusinessException("FORBIDDEN_CLASS_SCOPE",
                        "Chỉ được gửi thông báo cho lớp thuộc trường của mình", HttpStatus.FORBIDDEN);
            }
            enforceQuota(actorId);
            recipients = userAnnouncementPort.filterActiveUserIds(classAccessPort.findActiveMemberUserIds(classId));
            audience = "CLASS_ACTIVE";
            payload.put("class_id", classId.toString());
        } else if (SCOPE_SCHOOL.equals(scope) || scope == null) {
            UUID schoolId;
            if (superAdmin) {
                schoolId = request.getSchoolId();
                if (SCOPE_SCHOOL.equals(scope) && schoolId == null) {
                    throw invalidScope("SUPER_ADMIN dùng scope=SCHOOL phải truyền schoolId");
                }
            } else {
                schoolId = actorSchoolId;
                if (request.getSchoolId() != null && !request.getSchoolId().equals(actorSchoolId)) {
                    throw new BusinessException("FORBIDDEN_SCHOOL_SCOPE",
                            "Chỉ được gửi thông báo cho trường của mình", HttpStatus.FORBIDDEN);
                }
            }
            enforceQuota(actorId);
            if (schoolId == null) {
                recipients = userAnnouncementPort.findAllActiveUserIds();
                audience = "ALL_ACTIVE";
            } else {
                recipients = userAnnouncementPort.findActiveUserIdsBySchoolId(schoolId);
                audience = "SCHOOL_ACTIVE";
                payload.put("school_id", schoolId.toString());
            }
        } else {
            throw invalidScope("scope không hợp lệ: " + scope);
        }

        UUID announcementId = UUID.randomUUID();
        payload.put("entity_type", "system_announcement");
        payload.put("entity_id", announcementId.toString());
        payload.put("announced_by", actorId.toString());
        payload.put("audience", audience);

        int delivered = notificationCreationService.createForRecipients(
                NotificationType.SYSTEM_ANNOUNCEMENT,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                announcementId,
                recipients,
                request.getTitle(),
                request.getBody(),
                payload);
        return new AnnouncementResponse(announcementId, delivered);
    }

    private String normalizeScope(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String scope = raw.trim().toUpperCase(Locale.ROOT);
        if (!SCOPE_SCHOOL.equals(scope) && !SCOPE_CLASS.equals(scope)) {
            throw invalidScope("scope không hợp lệ: " + raw);
        }
        return scope;
    }

    private void enforceQuota(UUID actorId) {
        long sent = notificationRepository.countRecentAnnouncementsByActor(
                actorId.toString(), OffsetDateTime.now().minusHours(24));
        if (sent >= dailyLimit) {
            throw new BusinessException("ANNOUNCEMENT_RATE_LIMITED",
                    "Đã vượt quota " + dailyLimit + " announcement / 24h", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private BusinessException invalidScope(String message) {
        return new BusinessException("INVALID_ANNOUNCEMENT_SCOPE", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
