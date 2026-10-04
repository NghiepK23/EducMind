package com.educmind.analytics.service;

import com.educmind.analytics.dto.MasteryOverviewResponse;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/v1/courses/{courseId}/analytics/mastery-overview?class_id=
 * Thu tu kiem tra: quyen owner/dong giang day (403, course khong ton tai cung 403) -> truy van.
 * class_id tuy chon: khong truyen = ca course; co truyen = hoc sinh ACTIVE cua lop (source_class_id),
 * lop khong co hoc sinh -> items rong, 200.
 * Luu y: khi khong co class_id, mastery cua hoc sinh da roi course van duoc tinh (diem mo).
 */
@Service
public class TeacherMasteryOverviewQueryService {

    private final MasteryRepository masteryRepository;
    private final CourseAccessPort courseAccessPort;

    public TeacherMasteryOverviewQueryService(MasteryRepository masteryRepository,
                                              CourseAccessPort courseAccessPort) {
        this.masteryRepository = masteryRepository;
        this.courseAccessPort = courseAccessPort;
    }

    @Transactional(readOnly = true)
    public MasteryOverviewResponse find(UUID actorId, UUID courseId, UUID classId) {
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("FORBIDDEN_NOT_COURSE_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem mastery overview",
                    HttpStatus.FORBIDDEN);
        }

        List<MasteryRepository.OverviewRow> rows;
        if (classId == null) {
            rows = masteryRepository.summarizeByCourse(courseId);
        } else {
            List<UUID> studentIds = courseAccessPort
                    .findActiveStudentIdsByCourseAndSourceClass(courseId, classId);
            if (studentIds.isEmpty()) {
                return new MasteryOverviewResponse(courseId, classId, List.of());
            }
            rows = masteryRepository.summarizeByCourseAndStudents(courseId, studentIds);
        }

        List<MasteryOverviewResponse.Item> items = rows.stream()
                .map(MasteryOverviewResponse.Item::new).toList();
        return new MasteryOverviewResponse(courseId, classId, items);
    }
}
