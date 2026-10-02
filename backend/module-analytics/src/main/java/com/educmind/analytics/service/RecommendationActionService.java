package com.educmind.analytics.service;

import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Accept / dismiss recommendation của chính học sinh.
 * Module05 spec mục 5.1, 7; Module09 I.5.1 (dòng chưa duyệt bị ẩn với học sinh).
 * Accept không tự tạo attempt cho REATTEMPT_ASSESSMENT (Module09 I.5.4), chỉ đổi status.
 */
@Service
public class RecommendationActionService {

    private static final Set<String> VISIBLE_REVIEW_STATUSES =
            Set.of("NOT_REQUIRED", "APPROVED", "EDITED_AND_APPROVED");

    private final RecommendationRepository recommendationRepository;

    public RecommendationActionService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public Recommendation accept(UUID studentId, UUID recommendationId) {
        Recommendation recommendation = loadActionable(studentId, recommendationId);
        recommendation.accept();
        return recommendationRepository.save(recommendation);
    }

    @Transactional
    public Recommendation dismiss(UUID studentId, UUID recommendationId) {
        Recommendation recommendation = loadActionable(studentId, recommendationId);
        recommendation.dismiss();
        return recommendationRepository.save(recommendation);
    }

    private Recommendation loadActionable(UUID studentId, UUID recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .filter(r -> VISIBLE_REVIEW_STATUSES.contains(r.getReviewStatus()))
                .orElseThrow(() -> new BusinessException(
                        "RECOMMENDATION_NOT_FOUND", "Recommendation not found", HttpStatus.NOT_FOUND));

        if (!recommendation.getStudentId().equals(studentId)) {
            throw new BusinessException(
                    "FORBIDDEN_NOT_SELF", "Cannot act on another student's recommendation",
                    HttpStatus.FORBIDDEN);
        }

        String status = recommendation.getStatus();
        if (Recommendation.STATUS_EXPIRED.equals(status)) {
            throw new BusinessException(
                    "RECOMMENDATION_EXPIRED", "Recommendation has expired", HttpStatus.CONFLICT);
        }
        if (!Recommendation.STATUS_ACTIVE.equals(status)) {
            throw new BusinessException(
                    "RECOMMENDATION_ALREADY_PROCESSED", "Recommendation was already accepted or dismissed",
                    HttpStatus.CONFLICT);
        }
        return recommendation;
    }
}
