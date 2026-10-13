package com.educmind.rag.pipeline;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool luồng riêng của RAG. Cố tình KHÔNG đăng ký TaskExecutor thành bean để không
 * làm đổi executor mặc định của các @Async ở module khác.
 * Hàng đợi không giới hạn vì mỗi tác vụ chỉ mang một UUID. Luồng daemon: nếu app tắt giữa chừng,
 * resource đang xử lý sẽ kẹt ở PROCESSING cho đến khi reprocess (bước F).
 */
@Component
public class RagWorkerExecutor {

    private final ThreadPoolExecutor executor;

    public RagWorkerExecutor(RagSettings settings) {
        AtomicInteger counter = new AtomicInteger();
        int threads = Math.max(1, settings.workerThreads());
        this.executor = new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread t = new Thread(runnable, "rag-worker-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                });
    }

    /** Ném RejectedExecutionException nếu pool đã tắt. */
    public void submit(Runnable task) {
        executor.execute(task);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
