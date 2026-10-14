package com.educmind.rag.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.common.port.UserLookupPort;
import com.educmind.rag.dto.ReprocessResponse;
import com.educmind.rag.pipeline.RagSupportedTypes;
import com.educmind.rag.pipeline.RagWorkerExecutor;
import com.educmind.rag.pipeline.ResourceProcessingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * POST /api/v1/resources/{id}/reprocess (Module 6, spec D.7/D.10).
 * Quyền: giáo viên owner hoặc đồng giảng dạy. Không đánh dấu @Transactional ở đây:
 * việc đổi sang PROCESSING phải commit xong (trong cổng) trước khi giao cho worker.
 */
@Service
public class ResourceReprocessService {

    private static final Logger log = LoggerFactory.getLogger(ResourceReprocessService.class);

    private final ResourceProcessingPort resourcePort;
    private final CourseAccessPort courseAccessPort;
    private final UserLookupPort userLookupPort;
    private final RagWorkerExecutor executor;
    private final ResourceProcessingPipeline pipeline;
    private final Duration staleAfter;

    public ResourceReprocessService(ResourceProcessingPort resourcePort,
                                    CourseAccessPort courseAccessPort,
                                    UserLookupPort userLookupPort,
                                    RagWorkerExecutor executor,
                                    ResourceProcessingPipeline pipeline,
                                    @Value("${educmind.rag.processing-stale-minutes:30}") long staleMinutes) {
        this.resourcePort = resourcePort;
        this.courseAccessPort = courseAccessPort;
        this.userLookupPort = userLookupPort;
        this.executor = executor;
        this.pipeline = pipeline;
        this.staleAfter = Duration.ofMinutes(staleMinutes);
    }

    public ReprocessResponse reprocess(UUID actorId, UUID resourceId) {
        UserLookupPort.UserSummary actor = userLookupPort.findUserSummary(actorId)
                .orElseThrow(() -> new BusinessException(
                        "ACTOR_NOT_FOUND", "Không tìm thấy thông tin tài khoản của bạn", HttpStatus.UNAUTHORIZED));
        ResourceProcessingPort.ResourceInfo info = resourcePort.findResource(resourceId)
                .orElseThrow(this::notFound);

        if (!"TEACHER".equals(actor.role()) || !courseAccessPort.isOwnerOrCoTeacher(actorId, info.courseId())) {
            throw new BusinessException("REPROCESS_NOT_ALLOWED",
                    "Chỉ giáo viên sở hữu hoặc đồng giảng dạy khóa học mới được xử lý lại tài liệu",
                    HttpStatus.FORBIDDEN);
        }
        if (!RagSupportedTypes.isSupported(info.mimeType())) {
            throw new BusinessException("UNSUPPORTED_CONTENT_TYPE",
                    "Loại file này chưa được hỗ trợ để xử lý nội dung (chỉ PDF, DOCX, TXT, MD)",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!resourcePort.tryBeginReprocess(resourceId, staleAfter)) {
            if (resourcePort.findResource(resourceId).isEmpty()) {
                throw notFound();
            }
            throw new BusinessException("RESOURCE_ALREADY_PROCESSING",
                    "Tài liệu đang được xử lý, vui lòng đợi xử lý xong rồi thử lại", HttpStatus.CONFLICT);
        }

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
            resourcePort.markFailed(resourceId);
            throw new BusinessException("RAG_WORKER_UNAVAILABLE",
                    "Hệ thống xử lý tài liệu đang không sẵn sàng, vui lòng thử lại sau",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ReprocessResponse(resourceId, "PROCESSING");
    }

    private BusinessException notFound() {
        return new BusinessException("RESOURCE_NOT_FOUND", "Không tìm thấy tài liệu", HttpStatus.NOT_FOUND);
    }
}
