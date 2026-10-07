package com.educmind.notification.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.notification.dto.AnnouncementRequest;
import com.educmind.notification.dto.AnnouncementResponse;
import com.educmind.notification.dto.NotificationPageResult;
import com.educmind.notification.dto.NotificationResponse;
import com.educmind.notification.dto.PreferenceRequest;
import com.educmind.notification.dto.PreferenceResponse;
import com.educmind.notification.service.NotificationQueryService;
import com.educmind.notification.service.SystemAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class NotificationController {

    private final NotificationQueryService service;
    private final SystemAnnouncementService systemAnnouncementService;

    public NotificationController(NotificationQueryService service,
                                  SystemAnnouncementService systemAnnouncementService) {
        this.service = service;
        this.systemAnnouncementService = systemAnnouncementService;
    }

    @GetMapping("/api/v1/me/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            Authentication authentication,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "since_seq", required = false) Long sinceSeq,
            @RequestParam(name = "limit", required = false) Integer limit) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationPageResult r = service.list(userId, status, sinceSeq, limit);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("limit", r.limit());
        pagination.put("latest_seq", r.latestSeq());
        pagination.put("has_more", r.hasMore());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);
        return ResponseEntity.ok(ApiResponse.success(r.items(), meta));
    }

    @GetMapping("/api/v1/me/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unreadCount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unread_count", service.unreadCount(userId));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PatchMapping("/api/v1/me/notifications/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            Authentication authentication, @PathVariable("id") UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(service.markRead(userId, id)));
    }

    @PatchMapping("/api/v1/me/notifications/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("updated", service.markAllRead(userId));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/api/v1/me/notification-preferences")
    public ResponseEntity<ApiResponse<List<PreferenceResponse>>> listPreferences(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(service.listPreferences(userId)));
    }

    @PatchMapping("/api/v1/me/notification-preferences")
    public ResponseEntity<ApiResponse<PreferenceResponse>> setPreference(
            Authentication authentication, @Valid @RequestBody PreferenceRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                service.setPreference(userId, request.getNotificationType(), request.getEnabled())));
    }

    /** SCHOOL_ADMIN gửi trong trường của mình; SUPER_ADMIN gửi toàn hệ thống. */
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/api/v1/admin/notifications/announce")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> announce(
            Authentication authentication,
            @Valid @RequestBody AnnouncementRequest request) {
        UUID actorId = UUID.fromString(authentication.getName());
        boolean superAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(systemAnnouncementService.announce(actorId, superAdmin, request)));
    }
}
