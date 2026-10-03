package com.educmind.analytics.service;

import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * GET /api/v1/courses/{courseId}/analytics/knowledge-gaps?class_id=&status=&page=&size=
 * (Module05 FINAL muc 5.2 + PATCH3 Pagination CHUAN 1: offset page/size).
 *
 * Thu tu kiem tra: status khong hop le (400) -> quyen owner/dong giang day (403) -> truy van.
 * Course khong ton tai cung ra 403.
 * status khong truyen = tra tat ca trang thai.
 * class_id: loc theo hoc sinh ACTIVE co course_members.source_class_id = class_id;
 * lop khong co hoc sinh nao trong course -> trang rong, khong loi.
 * page < 1 -> 1; size < 1 -> mac dinh 20; size > 100 -> kep ve 100 (khong loi 422).
 */
@Service
public class TeacherKnowledgeGapQueryService {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;
    private static final Set<String> VALID_STATUSES = Set.of(
            KnowledgeGap.STATUS_OPEN, KnowledgeGap.STATUS_RESOLVED, KnowledgeGap.STATUS_DISMISSED);

    private final KnowledgeGapRepository knowledgeGapRepository;
    private final CourseAccessPort courseAccessPort;

    public TeacherKnowledgeGapQueryService(KnowledgeGapRepository knowledgeGapRepository,
                                           CourseAccessPort courseAccessPort) {
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.courseAccessPort = courseAccessPort;
    }

    public record Result(List<KnowledgeGapResponse> items, int page, int size,
                         long totalItems, int totalPages) {
    }

    @Transactional(readOnly = true)
    public Result find(UUID actorId, UUID courseId, UUID classId, String status,
                       Integer pageParam, Integer sizeParam) {
        if (status != null && !VALID_STATUSES.contains(status)) {
            throw new BusinessException("INVALID_FILTER",
                    "status phải là OPEN, RESOLVED hoặc DISMISSED", HttpStatus.BAD_REQUEST);
        }
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("FORBIDDEN_NOT_COURSE_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem knowledge gap",
                    HttpStatus.FORBIDDEN);
        }

        int page = (pageParam == null || pageParam < 1) ? 1 : pageParam;
        int size = (sizeParam == null || sizeParam < 1) ? DEFAULT_SIZE : Math.min(sizeParam, MAX_SIZE);
        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "openedAt").and(Sort.by("id")));

        Page<KnowledgeGap> result;
        if (classId == null) {
            result = (status == null)
                    ? knowledgeGapRepository.findByCourseId(courseId, pageable)
                    : knowledgeGapRepository.findByCourseIdAndStatus(courseId, status, pageable);
        } else {
            List<UUID> studentIds = courseAccessPort
                    .findActiveStudentIdsByCourseAndSourceClass(courseId, classId);
            if (studentIds.isEmpty()) {
                return new Result(List.of(), page, size, 0L, 0);
            }
            result = (status == null)
                    ? knowledgeGapRepository.findByCourseIdAndStudentIdIn(courseId, studentIds, pageable)
                    : knowledgeGapRepository.findByCourseIdAndStudentIdInAndStatus(
                            courseId, studentIds, status, pageable);
        }

        List<KnowledgeGapResponse> items = result.getContent().stream()
                .map(KnowledgeGapResponse::new).toList();
        return new Result(items, page, size, result.getTotalElements(), result.getTotalPages());
    }
}
