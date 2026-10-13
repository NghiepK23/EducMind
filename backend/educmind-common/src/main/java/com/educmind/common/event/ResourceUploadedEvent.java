package com.educmind.common.event;

import java.util.UUID;

/** Phát khi một resource mới được lưu và cần xử lý RAG (đọc chữ, tách đoạn, embedding). */
public record ResourceUploadedEvent(UUID resourceId) {
}
