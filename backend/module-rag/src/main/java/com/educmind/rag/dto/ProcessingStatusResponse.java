package com.educmind.rag.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Tiến trình xử lý RAG của một resource: trạng thái chung, số đoạn đã lưu, và từng giai đoạn. */
public record ProcessingStatusResponse(
        UUID resourceId,
        String filename,
        String processingStatus,
        int chunkCount,
        List<JobItem> jobs
) {
    public record JobItem(
            String stage,
            String status,
            int attemptCount,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
    }
}
