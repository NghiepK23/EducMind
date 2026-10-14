package com.educmind.rag.retrieval;

import com.educmind.common.port.AIProviderException;
import com.educmind.common.port.EmbeddingProvider;
import com.educmind.common.port.RetrievalPort;
import com.educmind.rag.store.ChunkSearchStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Truy xuất kết hợp: tìm theo vector + tìm theo từ khóa, gộp bằng RRF, trả top K.
 * Lỗi embedding câu hỏi (AIProviderException) được ném thẳng cho nơi gọi.
 */
@Service
public class HybridRetrievalService implements RetrievalPort {

    private final ChunkSearchStore store;
    private final EmbeddingProvider embeddingProvider;
    private final RetrievalSettings settings;

    public HybridRetrievalService(ChunkSearchStore store, EmbeddingProvider embeddingProvider,
                                  RetrievalSettings settings) {
        this.store = store;
        this.embeddingProvider = embeddingProvider;
        this.settings = settings;
    }

    @Override
    public List<RetrievedChunk> search(UUID courseId, UUID lessonId, String query) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId là bắt buộc");
        }
        if (query == null || query.isBlank()) {
            return List.of();
        }

        EmbeddingProvider.EmbeddingResult embedded =
                embeddingProvider.embed(List.of(query.strip()), EmbeddingProvider.Purpose.QUERY);
        if (embedded.vectors().size() != 1) {
            throw new AIProviderException("AI_BAD_RESPONSE",
                    "Embedding câu hỏi trả về " + embedded.vectors().size() + " vector, kỳ vọng 1");
        }

        List<ChunkSearchStore.Hit> vectorHits = store.searchByVector(
                courseId, lessonId, embedded.vectors().get(0), settings.vectorCandidates());

        String tsQuery = KeywordQueryBuilder.build(query, settings.keywordMaxTokens());
        List<ChunkSearchStore.Hit> keywordHits = tsQuery.isEmpty()
                ? List.of()
                : store.searchByKeyword(courseId, lessonId, tsQuery, settings.keywordCandidates());

        Map<UUID, ChunkSearchStore.Hit> byId = new HashMap<>();
        vectorHits.forEach(h -> byId.putIfAbsent(h.chunkId(), h));
        keywordHits.forEach(h -> byId.putIfAbsent(h.chunkId(), h));

        var fused = RrfFuser.fuse(
                List.of(vectorHits.stream().map(ChunkSearchStore.Hit::chunkId).toList(),
                        keywordHits.stream().map(ChunkSearchStore.Hit::chunkId).toList()),
                settings.rrfK(), settings.topK());

        return fused.stream().map(s -> {
            ChunkSearchStore.Hit h = byId.get(s.key());
            return new RetrievedChunk(h.chunkId(), h.resourceId(), h.lessonId(),
                    h.chunkIndex(), h.content(), s.score());
        }).toList();
    }
}
