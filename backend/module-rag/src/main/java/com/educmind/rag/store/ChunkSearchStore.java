package com.educmind.rag.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Các câu SQL tìm đoạn tài liệu (vector và từ khóa). Chỉ trả đoạn thuộc đúng course
 * và thuộc resource chưa bị xóa mềm. Kết quả đã sắp theo độ liên quan giảm dần.
 */
@Component
public class ChunkSearchStore {

    public record Hit(UUID chunkId, UUID resourceId, UUID lessonId, int chunkIndex, String content) {
    }

    private static final String SELECT_FROM =
            "SELECT c.id, c.resource_id, c.lesson_id, c.chunk_index, c.content "
                    + "FROM document_chunks c JOIN resources r ON r.id = c.resource_id "
                    + "WHERE c.course_id = ? AND r.deleted_at IS NULL";

    private static final RowMapper<Hit> HIT_MAPPER = (rs, i) -> new Hit(
            rs.getObject("id", UUID.class),
            rs.getObject("resource_id", UUID.class),
            rs.getObject("lesson_id", UUID.class),
            rs.getInt("chunk_index"),
            rs.getString("content"));

    private final JdbcTemplate jdbc;

    public ChunkSearchStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Gần nhất theo khoảng cách cosine. lessonId có thể null (không lọc theo lesson). */
    @Transactional(readOnly = true)
    public List<Hit> searchByVector(UUID courseId, UUID lessonId, float[] queryVector, int limit) {
        // Quét lặp để lọc course_id không làm chỉ mục HNSW trả thiếu kết quả (pgvector 0.8+)
        jdbc.execute("SET LOCAL hnsw.iterative_scan = strict_order");
        String sql = SELECT_FROM + (lessonId != null ? " AND c.lesson_id = ?" : "")
                + " ORDER BY c.embedding <=> ?::vector, c.chunk_index LIMIT ?";
        List<Object> args = new ArrayList<>();
        args.add(courseId);
        if (lessonId != null) {
            args.add(lessonId);
        }
        args.add(ChunkStore.toVectorLiteral(queryVector));
        args.add(limit);
        return jdbc.query(sql, HIT_MAPPER, args.toArray());
    }

    /** tsQuery là chuỗi đã dựng bởi KeywordQueryBuilder (từ nối bằng OR). */
    @Transactional(readOnly = true)
    public List<Hit> searchByKeyword(UUID courseId, UUID lessonId, String tsQuery, int limit) {
        String sql = SELECT_FROM + (lessonId != null ? " AND c.lesson_id = ?" : "")
                + " AND c.content_tsv @@ to_tsquery('simple', ?)"
                + " ORDER BY ts_rank(c.content_tsv, to_tsquery('simple', ?)) DESC, c.chunk_index LIMIT ?";
        List<Object> args = new ArrayList<>();
        args.add(courseId);
        if (lessonId != null) {
            args.add(lessonId);
        }
        args.add(tsQuery);
        args.add(tsQuery);
        args.add(limit);
        return jdbc.query(sql, HIT_MAPPER, args.toArray());
    }
}
