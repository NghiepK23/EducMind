package com.educmind.notification.dto;

import java.util.UUID;

/** Kết quả broadcast; số lượng đã tính cả notification preferences bị tắt. */
public record AnnouncementResponse(UUID announcementId, int deliveredCount) {
}
