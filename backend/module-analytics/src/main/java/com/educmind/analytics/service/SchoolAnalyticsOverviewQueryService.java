package com.educmind.analytics.service;

import com.educmind.analytics.dto.SchoolAnalyticsOverviewResponse;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.UserLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GET /api/v1/schools/{schoolId}/analytics/overview
 * Thu tu kiem tra: quyen (user active + schoolId trung school cua nguoi goi, neu khong 403) -> truy van.
 * Ban dau chi theo course (theo lop de ban sau). Moi course cua truong deu co 1 dong, ke ca khi chua co mastery.
 * Truy van gop theo courseId IN: 1 query mastery + 1 query dem gap OPEN, khong N+1.
 */
@Service
public class SchoolAnalyticsOverviewQueryService {

    private final UserLookupPort userLookupPort;
    private final CourseAccessPort courseAccessPort;
    private final MasteryRepository masteryRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;

    public SchoolAnalyticsOverviewQueryService(UserLookupPort userLookupPort,
                                               CourseAccessPort courseAccessPort,
                                               MasteryRepository masteryRepository,
                                               KnowledgeGapRepository knowledgeGapRepository) {
        this.userLookupPort = userLookupPort;
        this.courseAccessPort = courseAccessPort;
        this.masteryRepository = masteryRepository;
        this.knowledgeGapRepository = knowledgeGapRepository;
    }

    @Transactional(readOnly = true)
    public SchoolAnalyticsOverviewResponse find(UUID actorId, UUID schoolId) {
        var actor = userLookupPort.findUserSummary(actorId);
        if (actor.isEmpty() || !actor.get().active() || !schoolId.equals(actor.get().schoolId())) {
            throw new BusinessException("FORBIDDEN_NOT_SAME_SCHOOL",
                    "Chỉ SCHOOL_ADMIN của trường này mới được xem analytics tổng hợp của trường",
                    HttpStatus.FORBIDDEN);
        }

        List<UUID> courseIds = courseAccessPort.findCourseIdsBySchoolId(schoolId);
        if (courseIds.isEmpty()) {
            return new SchoolAnalyticsOverviewResponse(schoolId, List.of());
        }

        Map<UUID, MasteryRepository.CourseSummaryRow> masteryByCourse = new HashMap<>();
        for (MasteryRepository.CourseSummaryRow row : masteryRepository.summarizeBySchoolCourses(courseIds)) {
            masteryByCourse.put(row.getCourseId(), row);
        }

        Map<UUID, Long> openGapByCourse = new HashMap<>();
        for (KnowledgeGapRepository.CourseOpenGapRow row : knowledgeGapRepository.countOpenByCourseIds(courseIds)) {
            openGapByCourse.put(row.getCourseId(), row.getOpenCount());
        }

        List<SchoolAnalyticsOverviewResponse.Item> items = new ArrayList<>();
        for (UUID courseId : courseIds) {
            MasteryRepository.CourseSummaryRow m = masteryByCourse.get(courseId);
            long studentCount = m == null ? 0L : m.getStudentCount();
            Double avg = m == null ? null : m.getAvgMastery();
            long openGaps = openGapByCourse.getOrDefault(courseId, 0L);
            items.add(new SchoolAnalyticsOverviewResponse.Item(courseId, studentCount, avg, openGaps));
        }
        return new SchoolAnalyticsOverviewResponse(schoolId, items);
    }
}
