package com.educmind.notification.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.notification.dto.NotificationPageResult;
import com.educmind.notification.dto.NotificationResponse;
import com.educmind.notification.dto.PreferenceRequest;
import com.educmind.notification.dto.PreferenceResponse;
import com.educmind.notification.service.NotificationQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public NotificationController(NotificationQueryService service) {
        this.service = service;
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
}
