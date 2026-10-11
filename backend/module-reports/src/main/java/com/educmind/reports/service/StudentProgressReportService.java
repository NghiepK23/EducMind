package com.educmind.reports.service;

import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.dto.RecommendationResponse;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.reports.dto.StudentProgressReport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StudentProgressReportService {

    private final MasteryRepository masteryRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final RecommendationRepository recommendationRepository;
    private final CourseAccessPort courseAccessPort;

    public StudentProgressReportService(MasteryRepository masteryRepository,
                                        KnowledgeGapRepository knowledgeGapRepository,
                                        RecommendationRepository recommendationRepository,
                                        CourseAccessPort courseAccessPort) {
        this.masteryRepository = masteryRepository;
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.recommendationRepository = recommendationRepository;
        this.courseAccessPort = courseAccessPort;
    }

    @Transactional(readOnly = true)
    public StudentProgressReport generate(UUID actorId, UUID studentId, UUID courseId) {
        if (studentId == null || courseId == null) {
            throw new BusinessException("INVALID_FILTER",
                    "Cần truyền student_id và course_id", HttpStatus.BAD_REQUEST);
        }
        checkAccess(actorId, courseId);
        List<MasteryResponse> mastery = masteryRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .stream().map(MasteryResponse::new).toList();
        List<KnowledgeGapResponse> gaps = knowledgeGapRepository
                .findByStudentIdAndCourseIdOrderByOpenedAtDesc(studentId, courseId)
                .stream().map(KnowledgeGapResponse::new).toList();
        List<RecommendationResponse> recommendations = recommendationRepository
                .findByStudentIdAndCourseIdOrderByCreatedAtDesc(studentId, courseId)
                .stream().map(RecommendationResponse::new).toList();
        return new StudentProgressReport(studentId, courseId, OffsetDateTime.now(),
                mastery, gaps, recommendations);
    }

    public void checkAccess(UUID actorId, UUID courseId) {
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("REPORT_SCOPE_FORBIDDEN",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem báo cáo",
                    HttpStatus.FORBIDDEN);
        }
    }
}
