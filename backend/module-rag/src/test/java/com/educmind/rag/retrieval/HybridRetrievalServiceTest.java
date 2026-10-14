package com.educmind.rag.retrieval;

import com.educmind.common.port.AIProviderException;
import com.educmind.common.port.EmbeddingProvider;
import com.educmind.common.port.RetrievalPort;
import com.educmind.rag.store.ChunkSearchStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridRetrievalServiceTest {

    private final UUID courseId = UUID.randomUUID();
    private ChunkSearchStore store;
    private EmbeddingProvider embedding;

    @BeforeEach
    void setUp() {
        store = mock(ChunkSearchStore.class);
        embedding = mock(EmbeddingProvider.class);
        when(embedding.embed(anyList(), eq(EmbeddingProvider.Purpose.QUERY))).thenReturn(
                new EmbeddingProvider.EmbeddingResult(List.of(new float[]{1f}), "Gemini", "m", 1, 1L));
    }

    private HybridRetrievalService service(int topK) {
        return new HybridRetrievalService(store, embedding, new RetrievalSettings(20, 20, topK, 60, 20));
    }

    private static ChunkSearchStore.Hit hit(UUID id, int index) {
        return new ChunkSearchStore.Hit(id, UUID.randomUUID(), null, index, "nội dung " + index);
    }

    @Test
    void gopHaiNguon_phanTuCoMatOCaHaiXepDau() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(store.searchByVector(eq(courseId), isNull(), any(), eq(20)))
                .thenReturn(List.of(hit(a, 0), hit(b, 1)));
        when(store.searchByKeyword(eq(courseId), isNull(), eq("bộ | nhớ"), eq(20)))
                .thenReturn(List.of(hit(b, 1), hit(c, 2)));

        List<RetrievalPort.RetrievedChunk> result = service(5).search(courseId, null, "Bộ nhớ");

        assertEquals(List.of(b, a, c), result.stream().map(RetrievalPort.RetrievedChunk::chunkId).toList());
        assertEquals(1.0 / 61 + 1.0 / 62, result.get(0).score(), 1e-12);
    }

    @Test
    void giuDungTopK() {
        when(store.searchByVector(any(), any(), any(), anyInt()))
                .thenReturn(List.of(hit(UUID.randomUUID(), 0), hit(UUID.randomUUID(), 1), hit(UUID.randomUUID(), 2)));

        assertEquals(2, service(2).search(courseId, null, "Bộ nhớ").size());
    }

    @Test
    void cauHoiTrong_traRongVaKhongGoiEmbedding() {
        assertTrue(service(5).search(courseId, null, "   ").isEmpty());
        assertTrue(service(5).search(courseId, null, null).isEmpty());
        verify(embedding, never()).embed(anyList(), any());
    }

    @Test
    void khongCoTuKhoaDungDuoc_boQuaTimTuKhoa() {
        service(5).search(courseId, null, "?!");

        verify(store, never()).searchByKeyword(any(), any(), anyString(), anyInt());
    }

    @Test
    void truyenLessonIdXuongCaHaiTruyVan() {
        UUID lessonId = UUID.randomUUID();

        service(5).search(courseId, lessonId, "Bộ nhớ");

        verify(store).searchByVector(eq(courseId), eq(lessonId), any(), eq(20));
        verify(store).searchByKeyword(eq(courseId), eq(lessonId), eq("bộ | nhớ"), eq(20));
    }

    @Test
    void soVectorKhacMot_nemLoi() {
        when(embedding.embed(anyList(), any())).thenReturn(
                new EmbeddingProvider.EmbeddingResult(List.of(), "Gemini", "m", 1, 1L));

        assertThrows(AIProviderException.class, () -> service(5).search(courseId, null, "Bộ nhớ"));
    }

    @Test
    void thieuCourseId_nemLoi() {
        assertThrows(IllegalArgumentException.class, () -> service(5).search(null, null, "Bộ nhớ"));
    }
}
