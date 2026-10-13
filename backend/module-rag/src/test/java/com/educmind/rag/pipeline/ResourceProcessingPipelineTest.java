package com.educmind.rag.pipeline;

import com.educmind.common.port.AIProviderException;
import com.educmind.common.port.EmbeddingProvider;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.rag.extract.DocumentTextExtractor;
import com.educmind.rag.store.ChunkStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceProcessingPipelineTest {

    private static final int BATCH_SIZE = 2;

    private final UUID resourceId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID parseJob = UUID.randomUUID();
    private final UUID chunkingJob = UUID.randomUUID();
    private final UUID embeddingJob = UUID.randomUUID();

    private ResourceProcessingPort port;
    private DocumentTextExtractor extractor;
    private EmbeddingProvider embedding;
    private ChunkStore chunkStore;
    private JobRecorder jobs;
    private ResourceProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        port = mock(ResourceProcessingPort.class);
        extractor = mock(DocumentTextExtractor.class);
        embedding = mock(EmbeddingProvider.class);
        chunkStore = mock(ChunkStore.class);
        jobs = mock(JobRecorder.class);

        when(port.findResource(resourceId)).thenReturn(Optional.of(
                new ResourceProcessingPort.ResourceInfo(resourceId, courseId, null, "a.md", "text/markdown", 100)));
        when(port.openContent(resourceId)).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(jobs.startNew(resourceId)).thenReturn(new JobRecorder.JobIds(parseJob, chunkingJob, embeddingJob));

        // chunk nhỏ để văn bản ngắn cũng ra nhiều đoạn; chờ thử lại chỉ 1 ms cho test chạy nhanh
        RagSettings settings = new RagSettings(100, 150, 20, BATCH_SIZE, 3, 1L, 1);
        pipeline = new ResourceProcessingPipeline(port, extractor, embedding, chunkStore, jobs, settings);
    }

    private void textIs(String text) {
        when(extractor.extract(any(), anyString(), anyString())).thenReturn(text);
    }

    private static String longText() {
        String p = "Máy tính xử lí thông tin qua ba bước nhập xử lí và xuất kết quả cho người dùng.";
        return IntStream.range(0, 6).mapToObj(i -> p).collect(Collectors.joining("\n\n"));
    }

    private static EmbeddingProvider.EmbeddingResult resultFor(List<String> texts) {
        List<float[]> vectors = texts.stream().map(t -> new float[]{1f}).toList();
        return new EmbeddingProvider.EmbeddingResult(vectors, "Gemini", "test-model", 1, 1L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void nhieuChunk_chiaLoDungVaLuuDu() {
        textIs(longText());
        when(embedding.embed(anyList(), eq(EmbeddingProvider.Purpose.DOCUMENT)))
                .thenAnswer(inv -> resultFor(inv.getArgument(0)));

        pipeline.process(resourceId);

        ArgumentCaptor<List> stored = ArgumentCaptor.forClass(List.class);
        verify(chunkStore).replaceChunks(eq(resourceId), eq(courseId), any(), eq("test-model"), stored.capture());
        int n = stored.getValue().size();
        assertTrue(n >= 3, "kỳ vọng ít nhất 3 đoạn, thực tế " + n);
        int expectedBatches = (n + BATCH_SIZE - 1) / BATCH_SIZE;
        verify(embedding, times(expectedBatches)).embed(anyList(), eq(EmbeddingProvider.Purpose.DOCUMENT));
        verify(port).markReady(resourceId);
        verify(port, never()).markFailed(any());
        verify(jobs).succeeded(parseJob);
        verify(jobs).succeeded(chunkingJob);
        verify(jobs).succeeded(embeddingJob);
    }

    @Test
    void rateLimitMotLan_thuLaiRoiThanhCong() {
        textIs("Một đoạn ngắn.");
        when(embedding.embed(anyList(), any()))
                .thenThrow(new AIProviderException("AI_RATE_LIMITED", "429"))
                .thenAnswer(inv -> resultFor(inv.getArgument(0)));

        pipeline.process(resourceId);

        verify(embedding, times(2)).embed(anyList(), any());
        verify(port).markReady(resourceId);
        verify(port, never()).markFailed(any());
    }

    @Test
    void hetLuotThuLai_resourceFailed() {
        textIs("Một đoạn ngắn.");
        when(embedding.embed(anyList(), any())).thenThrow(new AIProviderException("AI_TIMEOUT", "hết giờ"));

        pipeline.process(resourceId);

        verify(embedding, times(3)).embed(anyList(), any());
        verify(port).markFailed(resourceId);
        verify(port, never()).markReady(any());
        verify(chunkStore, never()).replaceChunks(any(), any(), any(), any(), anyList());
        verify(jobs).failed(eq(embeddingJob), anyString());
    }

    @Test
    void loiKhongThuLaiDuoc_chiGoiMotLan() {
        textIs("Một đoạn ngắn.");
        when(embedding.embed(anyList(), any())).thenThrow(new AIProviderException("AI_AUTH_FAILED", "sai key"));

        pipeline.process(resourceId);

        verify(embedding, times(1)).embed(anyList(), any());
        verify(port).markFailed(resourceId);
    }

    @Test
    void soVectorLechSoDoan_resourceFailed() {
        textIs("Một đoạn ngắn.");
        when(embedding.embed(anyList(), any())).thenReturn(
                new EmbeddingProvider.EmbeddingResult(List.of(), "Gemini", "test-model", 1, 1L));

        pipeline.process(resourceId);

        verify(port).markFailed(resourceId);
        verify(chunkStore, never()).replaceChunks(any(), any(), any(), any(), anyList());
        verify(jobs, atLeastOnce()).failed(eq(embeddingJob), anyString());
    }

    @Test
    void resourceDaBiXoa_boQuaKhongLamGi() {
        when(port.findResource(resourceId)).thenReturn(Optional.empty());

        pipeline.process(resourceId);

        verify(jobs, never()).startNew(any());
        verify(port, never()).markProcessing(any());
        verify(port, never()).markReady(any());
        verify(port, never()).markFailed(any());
        assertEquals(0, mockingDetailInvocations());
    }

    private int mockingDetailInvocations() {
        return org.mockito.Mockito.mockingDetails(embedding).getInvocations().size();
    }
}
