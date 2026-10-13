package com.educmind.rag.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.common.port.UserLookupPort;
import com.educmind.rag.dto.ProcessingStatusResponse;
import com.educmind.rag.entity.ResourceProcessingJob;
import com.educmind.rag.repository.ResourceProcessingJobRepository;
import com.educmind.rag.store.ChunkStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * GET /api/v1/resources/{id}/processing-status (Module 6, spec D.7/D.10).
 * Quyền xem: giáo viên owner hoặc đồng giảng dạy; SCHOOL_ADMIN của đúng trường (chỉ xem).
 */
@Service
public class ResourceProcessingStatusService {

    private final ResourceProcessingPort resourcePort;
    private final CourseAccessPort courseAccessPort;
    private final UserLookupPort userLookupPort;
    private final ResourceProcessingJobRepository jobRepository;
    private final ChunkStore chunkStore;

    public ResourceProcessingStatusService(ResourceProcessingPort resourcePort,
                                           CourseAccessPort courseAccessPort,
                                           UserLookupPort userLookupPort,
                                           ResourceProcessingJobRepository jobRepository,
                                           ChunkStore chunkStore) {
        this.resourcePort = resourcePort;
        this.courseAccessPort = courseAccessPort;
        this.userLookupPort = userLookupPort;
        this.jobRepository = jobRepository;
        this.chunkStore = chunkStore;
    }

    @Transactional(readOnly = true)
    public ProcessingStatusResponse getStatus(UUID actorId, UUID resourceId) {
        UserLookupPort.UserSummary actor = userLookupPort.findUserSummary(actorId)
                .orElseThrow(() -> new BusinessException(
                        "ACTOR_NOT_FOUND", "Không tìm thấy thông tin tài khoản của bạn", HttpStatus.UNAUTHORIZED));
        ResourceProcessingPort.ResourceInfo info = resourcePort.findResource(resourceId)
                .orElseThrow(this::notFound);
        assertCanView(actor, actorId, info.courseId());

        String status = resourcePort.findProcessingStatus(resourceId).orElseThrow(this::notFound);

        List<ProcessingStatusResponse.JobItem> jobs = jobRepository.findByResourceIdOrderByCreatedAtAsc(resourceId)
                .stream()
                .sorted(Comparator.comparingInt((ResourceProcessingJob j) -> j.getStage().ordinal()))
                .map(j -> new ProcessingStatusResponse.JobItem(
                        j.getStage().name(), j.getStatus().name(), j.getAttemptCount(),
                        j.getErrorMessage(), j.getStartedAt(), j.getFinishedAt()))
                .toList();

        return new ProcessingStatusResponse(
                resourceId, info.filename(), status, chunkStore.countByResourceId(resourceId), jobs);
    }

    private void assertCanView(UserLookupPort.UserSummary actor, UUID actorId, UUID courseId) {
        boolean allowed;
        if ("TEACHER".equals(actor.role())) {
            allowed = courseAccessPort.isOwnerOrCoTeacher(actorId, courseId);
        } else if ("SCHOOL_ADMIN".equals(actor.role())) {
            allowed = actor.schoolId() != null
                    && courseAccessPort.findSchoolIdByCourseId(courseId).map(actor.schoolId()::equals).orElse(false);
        } else {
            allowed = false;
        }
        if (!allowed) {
            throw new BusinessException(
                    "NOT_COURSE_OWNER", "Bạn không có quyền xem tiến trình xử lý của tài liệu này", HttpStatus.FORBIDDEN);
        }
    }

    private BusinessException notFound() {
        return new BusinessException("RESOURCE_NOT_FOUND", "Không tìm thấy tài liệu", HttpStatus.NOT_FOUND);
    }
}
