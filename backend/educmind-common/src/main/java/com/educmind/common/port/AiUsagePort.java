package com.educmind.common.port;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Port cho module-reports lấy số liệu tổng hợp việc dùng AI của 1 trường trong 1 khoảng thời gian.
 * Chỉ trả số liệu, KHÔNG trả nội dung content/edited_content (PATCH 3: SCHOOL_ADMIN không xem nội dung).
 *
 * Hiện mới có dữ liệu Module 7 (ai_generation_*). Khi Module 9 xong (recommendation AI_DRAFT,
 * insight_reports) thì bổ sung thêm vào quality.
 */
public interface AiUsagePort {

    /**
     * @param generationRequestsCountByType số yêu cầu theo loại (luôn có đủ 3 loại, thiếu thì là 0)
     * @param pendingCount  số output đang chờ duyệt
     * @param approvedCount số output đã duyệt (APPROVED + EDITED_AND_APPROVED)
     * @param rejectedCount số output bị từ chối
     * @param approveRate   approved / (approved + rejected); 0 nếu chưa có output nào được duyệt/từ chối
     * @param rejectRate    rejected / (approved + rejected); 0 nếu chưa có output nào được duyệt/từ chối
     */
    record Quality(Map<String, Long> generationRequestsCountByType,
                   long pendingCount, long approvedCount, long rejectedCount,
                   double approveRate, double rejectRate) {
    }

    /**
     * @param errorRate số lượt gọi FAILED / tổng số lượt gọi; 0 nếu chưa có lượt nào
     */
    record Operational(long totalCalls, double avgLatencyMs,
                       BigDecimal totalEstimatedCost, double errorRate) {
    }

    record AiUsageSummary(UUID schoolId, OffsetDateTime periodStart, OffsetDateTime periodEnd,
                          Quality quality, Operational operational) {
    }

    /** Khoảng thời gian tính theo [periodStart, periodEnd). */
    AiUsageSummary summarizeBySchool(UUID schoolId, OffsetDateTime periodStart, OffsetDateTime periodEnd);
}
