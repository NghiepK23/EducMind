package com.educmind.analytics.service;

import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.dto.RecommendationResponse;
import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.pagination.PaginationSupport;
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
 * GET /students/me/knowledge-gaps va /students/me/recommendations (Module05 FINAL muc 5.1).
 * Phan trang offset (PATCH3 CHUAN 1). studentId lay tu token.
 * Recommendation chi tra dong da duoc phep hien thi cho hoc sinh (Module09 I.5.1):
 * review_status thuoc NOT_REQUIRED / APPROVED / EDITED_AND_APPROVED.
 */
@Service
public class StudentLearningQueryService {

    private static final Set<String> GAP_STATUSES = Set.of(
            KnowledgeGap.STATUS_OPEN, KnowledgeGap.STATUS_RESOLVED, KnowledgeGap.STATUS_DISMISSED);
    private static final Set<String> REC_STATUSES = Set.of(
            Recommendation.STATUS_ACTIVE, Recommendation.STATUS_ACCEPTED,
            Recommendation.STATUS_DISMISSED, Recommendation.STATUS_EXPIRED);
    private static final List<String> VISIBLE_REVIEW_STATUSES = List.of(
            Recommendation.REVIEW_NOT_REQUIRED, "APPROVED", "EDITED_AND_APPROVED");

    private final KnowledgeGapRepository gapRepository;
    private final RecommendationRepository recommendationRepository;

    public StudentLearningQueryService(KnowledgeGapRepository gapRepository,
                                       RecommendationRepository recommendationRepository) {
        this.gapRepository = gapRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeGapResponse> findMyGaps(UUID studentId, UUID courseId, String status,
                                                 Integer page, Integer size) {
        requireCourse(courseId);
        requireValidStatus(status, GAP_STATUSES);
        Pageable pageable = pageable(page, size, "openedAt");
        Page<KnowledgeGap> result = status == null
                ? gapRepository.findByStudentIdAndCourseId(studentId, courseId, pageable)
                : gapRepository.findByStudentIdAndCourseIdAndStatus(studentId, courseId, status, pageable);
        return result.map(KnowledgeGapResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<RecommendationResponse> findMyRecommendations(UUID studentId, UUID courseId,
                                                              String status, Integer page, Integer size) {
        requireCourse(courseId);
        requireValidStatus(status, REC_STATUSES);
        Pageable pageable = pageable(page, size, "createdAt");
        Page<Recommendation> result = status == null
                ? recommendationRepository.findByStudentIdAndCourseIdAndReviewStatusIn(
                        studentId, courseId, VISIBLE_REVIEW_STATUSES, pageable)
                : recommendationRepository.findByStudentIdAndCourseIdAndStatusAndReviewStatusIn(
                        studentId, courseId, status, VISIBLE_REVIEW_STATUSES, pageable);
        return result.map(RecommendationResponse::new);
    }

    private static Pageable pageable(Integer page, Integer size, String sortField) {
        return PageRequest.of(PaginationSupport.normalizePage(page) - 1,
                PaginationSupport.normalizeSize(size),
                Sort.by(Sort.Order.desc(sortField), Sort.Order.desc("id")));
    }

    private static void requireCourse(UUID courseId) {
        if (courseId == null) {
            throw new BusinessException("INVALID_FILTER", "Cần truyền tham số course_id",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static void requireValidStatus(String status, Set<String> allowed) {
        if (status != null && !allowed.contains(status)) {
            throw new BusinessException("INVALID_FILTER",
                    "status không hợp lệ, chỉ nhận: " + allowed, HttpStatus.BAD_REQUEST);
        }
    }
}
