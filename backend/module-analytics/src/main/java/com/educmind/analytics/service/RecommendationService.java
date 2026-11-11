package com.educmind.analytics.service;

import com.educmind.analytics.dto.RecommendationRow;
import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.entity.RecommendationStatus;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseContentPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** US-S06 — gợi ý học tập cá nhân hóa cho học sinh. */
@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final CourseContentPort contentPort;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 CourseContentPort contentPort) {
        this.recommendationRepository = recommendationRepository;
        this.contentPort = contentPort;
    }

    @Transactional(readOnly = true)
    public List<RecommendationRow> myRecommendations(UUID studentId, String status) {
        RecommendationStatus filter = parseStatus(status);
        List<Recommendation> recommendations = filter == null
                ? recommendationRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                : recommendationRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, filter);

        Map<UUID, CourseContentPort.ObjectiveInfo> infos = new HashMap<>();
        for (Recommendation recommendation : recommendations) {
            UUID objectiveId = recommendation.getObjectiveId();
            if (objectiveId != null && !infos.containsKey(objectiveId)) {
                contentPort.findObjectiveInfos(List.of(objectiveId)).stream().findFirst()
                        .ifPresent(info -> infos.put(objectiveId, info));
            }
        }

        List<RecommendationRow> result = new ArrayList<>();
        for (Recommendation recommendation : recommendations) {
            result.add(toRow(recommendation, infos.get(recommendation.getObjectiveId())));
        }
        return result;
    }

    /** AC-S06.3: bỏ qua gợi ý. */
    @Transactional
    public RecommendationRow dismiss(UUID studentId, UUID recommendationId) {
        Recommendation recommendation = requireOwn(studentId, recommendationId);
        recommendation.dismiss(OffsetDateTime.now());
        recommendationRepository.save(recommendation);
        return toRow(recommendation, objectiveInfo(recommendation));
    }

    /** AC-S06.3: học sinh mở nội dung được gợi ý (UI gọi trước khi điều hướng). */
    @Transactional
    public RecommendationRow accept(UUID studentId, UUID recommendationId) {
        Recommendation recommendation = requireOwn(studentId, recommendationId);
        recommendation.accept(OffsetDateTime.now());
        recommendationRepository.save(recommendation);
        return toRow(recommendation, objectiveInfo(recommendation));
    }

    private Recommendation requireOwn(UUID studentId, UUID recommendationId) {
        Recommendation recommendation = recommendationRepository
                .findByIdAndStudentId(recommendationId, studentId)
                .orElseThrow(() -> new BusinessException("RECOMMENDATION_NOT_FOUND",
                        "Không tìm thấy gợi ý học tập", HttpStatus.NOT_FOUND));
        if (recommendation.getStatus() != RecommendationStatus.ACTIVE) {
            throw new BusinessException("RECOMMENDATION_NOT_ACTIVE",
                    "Gợi ý này không còn hiệu lực", HttpStatus.CONFLICT);
        }
        return recommendation;
    }

    private CourseContentPort.ObjectiveInfo objectiveInfo(Recommendation recommendation) {
        if (recommendation.getObjectiveId() == null) {
            return null;
        }
        return contentPort.findObjectiveInfos(List.of(recommendation.getObjectiveId()))
                .stream().findFirst().orElse(null);
    }

    private RecommendationRow toRow(Recommendation recommendation, CourseContentPort.ObjectiveInfo info) {
        return new RecommendationRow(recommendation.getId(), recommendation.getType().name(),
                recommendation.getReason(),
                recommendation.getObjectiveId(),
                info == null ? null : info.code(),
                info == null ? null : info.description(),
                info == null ? null : info.lessonId(),
                info == null ? null : info.lessonTitle(),
                info != null && info.lessonPublished(),
                info == null ? null : info.topicId(),
                info == null ? null : info.topicTitle(),
                recommendation.getTargetEntityType(), recommendation.getTargetEntityId(),
                recommendation.getRelatedAssessmentId(), recommendation.getStatus().name(),
                recommendation.getCreatedAt(), recommendation.getRespondedAt());
    }

    private RecommendationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return RecommendationStatus.ACTIVE;
        }
        try {
            return RecommendationStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new BusinessException("INVALID_FILTER",
                    "Trạng thái không hợp lệ: " + value, HttpStatus.BAD_REQUEST);
        }
    }
}
