package com.educmind.rag.pipeline;

import com.educmind.rag.entity.JobStage;
import com.educmind.rag.entity.ResourceProcessingJob;
import com.educmind.rag.repository.ResourceProcessingJobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Ghi nhật ký từng giai đoạn. Mỗi hàm là một giao dịch riêng nên lưu được ngay cả khi giai đoạn sau lỗi. */
@Component
public class JobRecorder {

    public record JobIds(UUID parse, UUID chunking, UUID embedding) {
    }

    private final ResourceProcessingJobRepository repository;

    public JobRecorder(ResourceProcessingJobRepository repository) {
        this.repository = repository;
    }

    /** Xóa nhật ký cũ của resource rồi tạo 3 job mới ở trạng thái PENDING. */
    @Transactional
    public JobIds startNew(UUID resourceId) {
        repository.deleteByResourceId(resourceId);
        UUID parse = repository.save(new ResourceProcessingJob(resourceId, JobStage.PARSE)).getId();
        UUID chunking = repository.save(new ResourceProcessingJob(resourceId, JobStage.CHUNKING)).getId();
        UUID embedding = repository.save(new ResourceProcessingJob(resourceId, JobStage.EMBEDDING)).getId();
        return new JobIds(parse, chunking, embedding);
    }

    @Transactional
    public void running(UUID jobId) {
        repository.findById(jobId).ifPresent(ResourceProcessingJob::markRunning);
    }

    @Transactional
    public void succeeded(UUID jobId) {
        repository.findById(jobId).ifPresent(ResourceProcessingJob::markSucceeded);
    }

    @Transactional
    public void failed(UUID jobId, String errorMessage) {
        repository.findById(jobId).ifPresent(j -> j.markFailed(errorMessage));
    }
}
