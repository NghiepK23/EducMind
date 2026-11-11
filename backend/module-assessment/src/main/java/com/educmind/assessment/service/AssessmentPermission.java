package com.educmind.assessment.service;

import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentStatus;
import com.educmind.assessment.repository.AssessmentRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.CourseContentPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Quyền của module bài kiểm tra: giáo viên phụ trách lớp của khóa học; học sinh thuộc lớp
 * và chỉ thấy bài kiểm tra đã xuất bản (AC-T06.1, AC-T13.1).
 */
@Component
public class AssessmentPermission {

    public static final String ROLE_TEACHER = "TEACHER";

    private final AssessmentRepository assessmentRepository;
    private final CourseAccessPort courseAccessPort;
    private final CourseContentPort contentPort;

    public AssessmentPermission(AssessmentRepository assessmentRepository,
                                CourseAccessPort courseAccessPort,
                                CourseContentPort contentPort) {
        this.assessmentRepository = assessmentRepository;
        this.courseAccessPort = courseAccessPort;
        this.contentPort = contentPort;
    }

    public static boolean isTeacher(String role) {
        return ROLE_TEACHER.equals(role);
    }

    public void requireManagedCourse(UUID teacherId, UUID courseId) {
        if (!courseAccessPort.isOwner(teacherId, courseId)) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Bạn không phụ trách lớp của khóa học này", HttpStatus.FORBIDDEN);
        }
    }

    public Assessment requireManagedAssessment(UUID teacherId, UUID assessmentId) {
        Assessment assessment = assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)
                .orElseThrow(() -> notFound("Không tìm thấy bài kiểm tra"));
        requireManagedCourse(teacherId, assessment.getCourseId());
        return assessment;
    }

    public Assessment requireVisibleAssessment(UUID actorId, String role, UUID assessmentId) {
        Assessment assessment = assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)
                .orElseThrow(() -> notFound("Không tìm thấy bài kiểm tra"));

        if (isTeacher(role)) {
            requireManagedCourse(actorId, assessment.getCourseId());
            return assessment;
        }

        if (!courseAccessPort.isStudentEnrolled(actorId, assessment.getCourseId())) {
            throw new BusinessException("COURSE_ACCESS_DENIED",
                    "Bạn không có quyền truy cập khóa học này", HttpStatus.FORBIDDEN);
        }
        requirePublishedForStudent(assessment);
        return assessment;
    }

    public void requirePublishedForStudent(Assessment assessment) {
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new BusinessException("ASSESSMENT_NOT_PUBLISHED",
                    "Bài kiểm tra chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
        if (assessment.getTopicId() != null && !contentPort.isTopicPublished(assessment.getTopicId())) {
            throw new BusinessException("ASSESSMENT_NOT_PUBLISHED",
                    "Chủ đề của bài kiểm tra chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
        if (assessment.getLessonId() != null && !contentPort.isLessonPublished(assessment.getLessonId())) {
            throw new BusinessException("ASSESSMENT_NOT_PUBLISHED",
                    "Bài học của bài kiểm tra chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
    }

    public BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
