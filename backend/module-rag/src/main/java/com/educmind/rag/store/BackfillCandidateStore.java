package com.educmind.rag.store;

import com.educmind.rag.pipeline.RagSupportedTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Liệt kê resource cần xử lý bù (backfill) bằng SQL thuần, join bảng resources của module-course
 * (cùng cách ChunkSearchStore đang làm).
 */
@Component
public class BackfillCandidateStore {

    private final JdbcTemplate jdbc;

    public BackfillCandidateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Resource chưa xóa mềm, thuộc loại RAG đọc được, và rơi vào một trong hai nhóm:
     * READY mà chưa có chunk nào (tài liệu cũ), hoặc PROCESSING quá staleMinutes phút (bị kẹt).
     * Sắp theo thời điểm tạo, cũ trước.
     */
    public List<UUID> findCandidateIds(long staleMinutes) {
        List<String> mimes = List.copyOf(RagSupportedTypes.MIME_TYPES);
        String placeholders = String.join(", ", Collections.nCopies(mimes.size(), "?"));
        String sql = "SELECT r.id FROM resources r "
                + "WHERE r.deleted_at IS NULL "
                + "AND r.mime_type IN (" + placeholders + ") "
                + "AND ((r.processing_status = 'READY' "
                + "      AND NOT EXISTS (SELECT 1 FROM document_chunks c WHERE c.resource_id = r.id)) "
                + "  OR (r.processing_status = 'PROCESSING' "
                + "      AND r.updated_at < now() - (? * interval '1 minute'))) "
                + "ORDER BY r.created_at";
        List<Object> args = new ArrayList<>(mimes);
        args.add(staleMinutes);
        return jdbc.query(sql, (rs, i) -> rs.getObject("id", UUID.class), args.toArray());
    }
}
