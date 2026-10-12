package com.educmind.rag.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ghi/xóa document_chunks bằng SQL thuần (JdbcTemplate), không dùng entity JPA,
 * vì cột vector cần kiểu riêng của Hibernate mà ta chưa đưa vào.
 */
@Component
public class ChunkStore {

    /** Một đoạn chữ đã có vector, sẵn sàng ghi. metadataJson có thể null. */
    public record NewChunk(int chunkIndex, String content, float[] embedding, String metadataJson) {
    }

    private final JdbcTemplate jdbc;

    public ChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Xóa chunk cũ của resource rồi ghi chunk mới, trong cùng một giao dịch. */
    @Transactional
    public void replaceChunks(UUID resourceId, UUID courseId, UUID lessonId,
                              String embeddingModel, List<NewChunk> chunks) {
        deleteByResourceId(resourceId);
        List<Object[]> rows = new ArrayList<>(chunks.size());
        for (NewChunk c : chunks) {
            rows.add(new Object[]{
                    resourceId, courseId, lessonId, c.chunkIndex(), c.content(),
                    toVectorLiteral(c.embedding()), embeddingModel, c.metadataJson()
            });
        }
        jdbc.batchUpdate(
                "INSERT INTO document_chunks "
                        + "(resource_id, course_id, lesson_id, chunk_index, content, embedding, embedding_model, metadata) "
                        + "VALUES (?, ?, ?, ?, ?, ?::vector, ?, ?::jsonb)",
                rows);
    }

    public int deleteByResourceId(UUID resourceId) {
        return jdbc.update("DELETE FROM document_chunks WHERE resource_id = ?", resourceId);
    }

    public int countByResourceId(UUID resourceId) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM document_chunks WHERE resource_id = ?", Integer.class, resourceId);
        return n == null ? 0 : n;
    }

    /** Định dạng pgvector chấp nhận: "[0.1,0.2,...]". */
    static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10 + 2);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
