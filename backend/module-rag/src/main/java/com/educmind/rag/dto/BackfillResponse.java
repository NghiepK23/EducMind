package com.educmind.rag.dto;

/** Số resource đã được xếp hàng xử lý bù (0 nếu không còn resource nào cần xử lý). */
public record BackfillResponse(int queued) {
}
