package com.educmind.notification.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.notification.dto.NotificationResponse;
import com.educmind.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** US-G03 — mọi endpoint chỉ trả dữ liệu của chính người dùng đang đăng nhập. */
@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            Principal principal,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {

        NotificationService.NotificationPage result =
                service.list(UUID.fromString(principal.getName()), page, size);

        return ResponseEntity.ok(ApiResponse.success(result.items(), result.meta()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(Principal principal) {
        long count = service.unreadCount(UUID.fromString(principal.getName()));
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(Principal principal,
                                                                     @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.markRead(UUID.fromString(principal.getName()), id)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(Principal principal) {
        int updated = service.markAllRead(UUID.fromString(principal.getName()));
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated)));
    }
}
