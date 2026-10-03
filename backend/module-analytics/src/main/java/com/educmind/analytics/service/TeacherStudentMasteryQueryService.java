package com.educmind.analytics.service;

import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/v1/students/{studentId}/mastery?course_id= (Module05 FINAL muc 5.2).
 *
 * Quyen: owner HOAC dong giang day cua course (courseAccessPort.isOwnerOrCoTeacher),
 * cung quy tac voi KnowledgeGapDismissService.
 *
 * Thu tu kiem tra: course_id thieu (400) -> quyen (403) -> truy van.
 * Course khong ton tai cung ra 403 de khong lo su ton tai cua course.
 * Khong kiem tra hoc sinh co thuoc course hay khong: du lieu da loc theo course_id
 * ma giao vien co quyen, hoc sinh ngoai course tra danh sach rong.
 */
@Service
public class TeacherStudentMasteryQueryService {

    private final MasteryRepository masteryRepository;
    private final CourseAccessPort courseAccessPort;

    public TeacherStudentMasteryQueryService(MasteryRepository masteryRepository,
                                             CourseAccessPort courseAccessPort) {
        this.masteryRepository = masteryRepository;
        this.courseAccessPort = courseAccessPort;
    }

    @Transactional(readOnly = true)
    public List<MasteryResponse> find(UUID actorId, UUID studentId, UUID courseId) {
        if (courseId == null) {
            throw new BusinessException("INVALID_FILTER",
                    "Cần truyền tham số course_id", HttpStatus.BAD_REQUEST);
        }
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("FORBIDDEN_NOT_COURSE_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem mastery của học sinh",
                    HttpStatus.FORBIDDEN);
        }
        return masteryRepository.findByStudentIdAndCourseId(studentId, courseId)
                .stream().map(MasteryResponse::new).toList();
    }
}
