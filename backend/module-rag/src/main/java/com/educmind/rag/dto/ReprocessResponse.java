package com.educmind.rag.dto;

import java.util.UUID;

/** Kết quả nhận yêu cầu xử lý lại: resource đã được đưa về PROCESSING và giao cho worker. */
public record ReprocessResponse(UUID resourceId, String processingStatus) {
}
