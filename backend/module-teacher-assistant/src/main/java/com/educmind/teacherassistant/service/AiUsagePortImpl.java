package com.educmind.teacherassistant.service;

import com.educmind.common.port.AiUsagePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tính số liệu tổng hợp dùng AI theo trường. Bảng log/yêu cầu không có school_id,
 * nên JOIN users để lọc theo users.school_id của người yêu cầu (requested_by) / người gọi (actor_id).
 */
@Service
public class AiUsagePortImpl implements AiUsagePort {

    private static final List<String> REQUEST_TYPES = List.of("LESSON_PLAN", "QUESTION_SET", "ASSESSMENT_MATRIX");

    private final JdbcTemplate jdbc;

    public AiUsagePortImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageSummary summarizeBySchool(UUID schoolId, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : REQUEST_TYPES) {
            byType.put(t, 0L);
        }
        jdbc.query("""
                select r.request_type, count(*)
                from ai_generation_requests r
                join users u on u.id = r.requested_by
                where u.school_id = ? and r.created_at >= ? and r.created_at < ?
                group by r.request_type
                """, rs -> {
            byType.put(rs.getString(1), rs.getLong(2));
        }, schoolId, periodStart, periodEnd);

        long[] review = new long[3];
        jdbc.query("""
                select o.review_status, count(*)
                from ai_generation_outputs o
                join ai_generation_requests r on r.id = o.request_id
                join users u on u.id = r.requested_by
                where u.school_id = ? and r.created_at >= ? and r.created_at < ?
                group by o.review_status
                """, rs -> {
            String status = rs.getString(1);
            long n = rs.getLong(2);
            switch (status) {
                case "PENDING_REVIEW" -> review[0] += n;
                case "APPROVED", "EDITED_AND_APPROVED" -> review[1] += n;
                case "REJECTED" -> review[2] += n;
                default -> {
                }
            }
        }, schoolId, periodStart, periodEnd);

        long pending = review[0];
        long approved = review[1];
        long rejected = review[2];
        long reviewed = approved + rejected;
        Quality quality = new Quality(byType, pending, approved, rejected,
                reviewed == 0 ? 0.0 : round((double) approved / reviewed, 4),
                reviewed == 0 ? 0.0 : round((double) rejected / reviewed, 4));

        Operational operational = jdbc.queryForObject("""
                select count(*) as total,
                       coalesce(avg(l.latency_ms), 0) as avg_latency,
                       coalesce(sum(l.estimated_cost), 0) as cost,
                       count(*) filter (where l.status = 'FAILED') as failed
                from ai_observability_logs l
                join users u on u.id = l.actor_id
                where u.school_id = ? and l.created_at >= ? and l.created_at < ?
                """, (rs, rowNum) -> {
            long total = rs.getLong("total");
            long failed = rs.getLong("failed");
            return new Operational(total, round(rs.getDouble("avg_latency"), 2),
                    rs.getBigDecimal("cost"),
                    total == 0 ? 0.0 : round((double) failed / total, 4));
        }, schoolId, periodStart, periodEnd);

        return new AiUsageSummary(schoolId, periodStart, periodEnd, quality, operational);
    }

    private static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
