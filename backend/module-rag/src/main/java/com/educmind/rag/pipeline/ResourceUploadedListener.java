package com.educmind.rag.pipeline;

import com.educmind.common.event.ResourceUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/** Nhận sự kiện sau khi giao dịch tạo resource đã commit, rồi giao cho worker xử lý nền. */
@Component
public class ResourceUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceUploadedListener.class);

    private final RagWorkerExecutor executor;
    private final ResourceProcessingPipeline pipeline;

    public ResourceUploadedListener(RagWorkerExecutor executor, ResourceProcessingPipeline pipeline) {
        this.executor = executor;
        this.pipeline = pipeline;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onResourceUploaded(ResourceUploadedEvent event) {
        UUID resourceId = event.resourceId();
        try {
            executor.submit(() -> {
                try {
                    pipeline.process(resourceId);
                } catch (Throwable t) {
                    log.error("Worker RAG gặp lỗi với resource " + resourceId, t);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Không giao được resource {} cho worker RAG (pool đã tắt)", resourceId);
        }
    }
}
