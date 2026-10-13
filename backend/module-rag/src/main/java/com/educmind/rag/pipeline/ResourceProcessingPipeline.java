package com.educmind.rag.pipeline;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.AIProviderException;
import com.educmind.common.port.EmbeddingProvider;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.rag.chunk.TextChunker;
import com.educmind.rag.extract.DocumentTextExtractor;
import com.educmind.rag.store.ChunkStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Chạy pipeline RAG cho một resource: PARSE -> CHUNKING -> EMBEDDING -> lưu chunk -> READY.
 * Hàm process không ném ngoại lệ ra ngoài: lỗi được ghi vào resource_processing_jobs và resource chuyển FAILED.
 */
@Service
public class ResourceProcessingPipeline {

    private static final Logger log = LoggerFactory.getLogger(ResourceProcessingPipeline.class);
    private static final Set<String> RETRYABLE_CODES = Set.of("AI_RATE_LIMITED", "AI_TIMEOUT", "AI_UNAVAILABLE");
    private static final int MAX_ERROR_LENGTH = 1000;

    private final ResourceProcessingPort resourcePort;
    private final DocumentTextExtractor extractor;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkStore chunkStore;
    private final JobRecorder jobs;
    private final RagSettings settings;
    private final TextChunker chunker;

    public ResourceProcessingPipeline(ResourceProcessingPort resourcePort,
                                      DocumentTextExtractor extractor,
                                      EmbeddingProvider embeddingProvider,
                                      ChunkStore chunkStore,
                                      JobRecorder jobs,
                                      RagSettings settings) {
        this.resourcePort = resourcePort;
        this.extractor = extractor;
        this.embeddingProvider = embeddingProvider;
        this.chunkStore = chunkStore;
        this.jobs = jobs;
        this.settings = settings;
        this.chunker = new TextChunker(settings.chunkTargetChars(), settings.chunkMaxChars(),
                settings.chunkOverlapChars());
    }

    public void process(UUID resourceId) {
        try {
            doProcess(resourceId);
        } catch (StageFailedException e) {
            log.warn("Xử lý resource {} thất bại: {}", resourceId, e.getCause().getMessage());
            safeMarkFailed(resourceId);
        } catch (RuntimeException e) {
            log.error("Lỗi không mong đợi khi xử lý resource " + resourceId, e);
            safeMarkFailed(resourceId);
        }
    }

    private void doProcess(UUID resourceId) {
        Optional<ResourceProcessingPort.ResourceInfo> found = resourcePort.findResource(resourceId);
        if (found.isEmpty()) {
            log.info("Bỏ qua resource {}: không còn tồn tại hoặc đã bị xóa", resourceId);
            return;
        }
        ResourceProcessingPort.ResourceInfo info = found.get();
        JobRecorder.JobIds jobIds = jobs.startNew(resourceId);
        resourcePort.markProcessing(resourceId);

        String text = runStage(jobIds.parse(), () -> {
            try (InputStream in = resourcePort.openContent(resourceId)) {
                return extractor.extract(in, info.filename(), info.mimeType());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        var chunks = runStage(jobIds.chunking(), () -> {
            var result = chunker.chunk(text);
            if (result.isEmpty()) {
                throw new BusinessException("UNSUPPORTED_CONTENT_TYPE",
                        "Không tách được đoạn chữ nào từ tài liệu", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return result;
        });

        runStage(jobIds.embedding(), () -> {
            List<ChunkStore.NewChunk> toStore = new ArrayList<>(chunks.size());
            String model = null;
            int batchSize = Math.max(1, settings.embeddingBatchSize());
            for (int from = 0; from < chunks.size(); from += batchSize) {
                var batch = chunks.subList(from, Math.min(from + batchSize, chunks.size()));
                List<String> texts = batch.stream().map(c -> c.content()).toList();
                EmbeddingProvider.EmbeddingResult result = embedWithRetry(texts, jobIds.embedding());
                if (result.vectors().size() != texts.size()) {
                    throw new AIProviderException("AI_BAD_RESPONSE",
                            "Số vector trả về (" + result.vectors().size() + ") khác số đoạn gửi (" + texts.size() + ")");
                }
                model = result.model();
                for (int i = 0; i < batch.size(); i++) {
                    toStore.add(new ChunkStore.NewChunk(batch.get(i).index(), batch.get(i).content(),
                            result.vectors().get(i), null));
                }
            }
            chunkStore.replaceChunks(info.id(), info.courseId(), info.lessonId(), model, toStore);
            return null;
        });

        resourcePort.markReady(resourceId);
        if (resourcePort.findResource(resourceId).isEmpty()) {
            chunkStore.deleteByResourceId(resourceId);
        }
        log.info("Đã xử lý resource {}: {} đoạn", resourceId, chunks.size());
    }

    private <T> T runStage(UUID jobId, Supplier<T> body) {
        jobs.running(jobId);
        try {
            T result = body.get();
            jobs.succeeded(jobId);
            return result;
        } catch (RuntimeException e) {
            jobs.failed(jobId, describe(e));
            throw new StageFailedException(e);
        }
    }

    private EmbeddingProvider.EmbeddingResult embedWithRetry(List<String> texts, UUID embeddingJobId) {
        long backoff = settings.retryBackoffMillis();
        for (int attempt = 1; ; attempt++) {
            try {
                return embeddingProvider.embed(texts, EmbeddingProvider.Purpose.DOCUMENT);
            } catch (AIProviderException e) {
                if (!RETRYABLE_CODES.contains(e.getCode()) || attempt >= settings.maxAttempts()) {
                    throw e;
                }
                log.warn("Embedding lỗi {} (lần {}/{}), thử lại sau {} ms",
                        e.getCode(), attempt, settings.maxAttempts(), backoff);
                sleep(backoff);
                backoff *= 2;
                jobs.running(embeddingJobId);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Worker bị ngắt khi đang chờ thử lại", e);
        }
    }

    private void safeMarkFailed(UUID resourceId) {
        try {
            resourcePort.markFailed(resourceId);
        } catch (RuntimeException e) {
            log.error("Không đánh dấu FAILED được cho resource " + resourceId, e);
        }
    }

    private static String describe(RuntimeException e) {
        String code = null;
        if (e instanceof AIProviderException a) {
            code = a.getCode();
        } else if (e instanceof BusinessException b) {
            code = b.getErrorCode();
        }
        String text = (code != null ? code : e.getClass().getSimpleName()) + " - " + e.getMessage();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }

    private static final class StageFailedException extends RuntimeException {
        StageFailedException(Throwable cause) {
            super(cause);
        }
    }
}
