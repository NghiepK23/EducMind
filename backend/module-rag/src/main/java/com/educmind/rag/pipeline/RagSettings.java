package com.educmind.rag.pipeline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Các hằng số vận hành của pipeline RAG, đọc từ educmind.rag.* (có giá trị mặc định). */
@Component
public class RagSettings {

    private final int chunkTargetChars;
    private final int chunkMaxChars;
    private final int chunkOverlapChars;
    private final int embeddingBatchSize;
    private final int maxAttempts;
    private final long retryBackoffMillis;
    private final int workerThreads;

    public RagSettings(
            @Value("${educmind.rag.chunk.target-chars:1200}") int chunkTargetChars,
            @Value("${educmind.rag.chunk.max-chars:1800}") int chunkMaxChars,
            @Value("${educmind.rag.chunk.overlap-chars:150}") int chunkOverlapChars,
            @Value("${educmind.rag.embedding-batch-size:16}") int embeddingBatchSize,
            @Value("${educmind.rag.retry.max-attempts:3}") int maxAttempts,
            @Value("${educmind.rag.retry.backoff-millis:2000}") long retryBackoffMillis,
            @Value("${educmind.rag.worker-threads:2}") int workerThreads) {
        this.chunkTargetChars = chunkTargetChars;
        this.chunkMaxChars = chunkMaxChars;
        this.chunkOverlapChars = chunkOverlapChars;
        this.embeddingBatchSize = embeddingBatchSize;
        this.maxAttempts = maxAttempts;
        this.retryBackoffMillis = retryBackoffMillis;
        this.workerThreads = workerThreads;
    }

    public int chunkTargetChars() {
        return chunkTargetChars;
    }

    public int chunkMaxChars() {
        return chunkMaxChars;
    }

    public int chunkOverlapChars() {
        return chunkOverlapChars;
    }

    public int embeddingBatchSize() {
        return embeddingBatchSize;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public long retryBackoffMillis() {
        return retryBackoffMillis;
    }

    public int workerThreads() {
        return workerThreads;
    }
}
