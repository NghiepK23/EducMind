package com.educmind.rag.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.rag.dto.BackfillResponse;
import com.educmind.rag.pipeline.RagWorkerExecutor;
import com.educmind.rag.pipeline.ResourceProcessingPipeline;
import com.educmind.rag.store.BackfillCandidateStore;
import com.educmind.rag.store.ChunkStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Xử lý bù các resource cũ chưa có chunk và resource bị kẹt PROCESSING (Module 6, bước F2).
 * Chạy tuần tự trong MỘT tác vụ nền, nghỉ giữa các resource để bớt dồn vào hạn mức embedding.
 * Cờ running chỉ có tác dụng trong một tiến trình app.
 */
@Service
public class RagBackfillService {

    private static final Logger log = LoggerFactory.getLogger(RagBackfillService.class);

    private final BackfillCandidateStore candidates;
    private final ResourceProcessingPort resourcePort;
    private final ChunkStore chunkStore;
    private final RagWorkerExecutor executor;
    private final ResourceProcessingPipeline pipeline;
    private final Duration staleAfter;
    private final long pauseMillis;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RagBackfillService(BackfillCandidateStore candidates,
                              ResourceProcessingPort resourcePort,
                              ChunkStore chunkStore,
                              RagWorkerExecutor executor,
                              ResourceProcessingPipeline pipeline,
                              @Value("${educmind.rag.processing-stale-minutes:30}") long staleMinutes,
                              @Value("${educmind.rag.backfill.pause-millis:1000}") long pauseMillis) {
        this.candidates = candidates;
        this.resourcePort = resourcePort;
        this.chunkStore = chunkStore;
        this.executor = executor;
        this.pipeline = pipeline;
        this.staleAfter = Duration.ofMinutes(staleMinutes);
        this.pauseMillis = pauseMillis;
    }

    public BackfillResponse start() {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException("BACKFILL_ALREADY_RUNNING",
                    "Đang có một đợt xử lý bù chạy, vui lòng đợi nó xong", HttpStatus.CONFLICT);
        }
        List<UUID> ids;
        try {
            ids = candidates.findCandidateIds(staleAfter.toMinutes());
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
        if (ids.isEmpty()) {
            running.set(false);
            return new BackfillResponse(0);
        }
        try {
            executor.submit(() -> runAll(ids));
        } catch (RejectedExecutionException e) {
            running.set(false);
            log.error("Không giao được đợt backfill cho worker RAG (pool đã tắt)");
            throw new BusinessException("RAG_WORKER_UNAVAILABLE",
                    "Hệ thống xử lý tài liệu đang không sẵn sàng, vui lòng thử lại sau",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        log.info("Backfill RAG: xếp hàng {} resource", ids.size());
        return new BackfillResponse(ids.size());
    }

    private void runAll(List<UUID> ids) {
        try {
            int done = 0;
            for (UUID id : ids) {
                if (processOne(id)) {
                    done++;
                    if (!pause()) {
                        break;
                    }
                }
            }
            log.info("Backfill RAG xong: đã xử lý {}/{} resource", done, ids.size());
        } catch (Throwable t) {
            log.error("Backfill RAG gặp lỗi không mong đợi", t);
        } finally {
            running.set(false);
        }
    }

    /** Trả true nếu đã chạy pipeline cho resource này. */
    private boolean processOne(UUID id) {
        Optional<String> status = resourcePort.findProcessingStatus(id);
        if (status.isEmpty()) {
            return false;
        }
        if ("READY".equals(status.get()) && chunkStore.countByResourceId(id) > 0) {
            return false;
        }
        if (!resourcePort.tryBeginReprocess(id, staleAfter)) {
            return false;
        }
        try {
            pipeline.process(id);
        } catch (Throwable t) {
            log.error("Backfill: lỗi khi xử lý resource " + id, t);
        }
        return true;
    }

    /** Trả false nếu luồng bị ngắt (app đang tắt) và nên dừng cả đợt. */
    private boolean pause() {
        if (pauseMillis <= 0) {
            return true;
        }
        try {
            Thread.sleep(pauseMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
