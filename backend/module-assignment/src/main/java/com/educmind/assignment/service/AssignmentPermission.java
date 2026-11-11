package com.educmind.assignment.service;

import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentStatus;
import com.educmind.assignment.repository.AssignmentRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.CourseContentPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tập trung kiểm tra quyền của module bài tập.
 * Quyền quản lý = phụ trách lớp của khóa học (classes.owner_teacher_id);
 * học sinh xem được bài tập đã xuất bản thuộc khóa học của lớp mình (AC-T03.4, AC-S02.5).
 */
@Component
public class AssignmentPermission {

    public static final String ROLE_TEACHER = "TEACHER";

    private final AssignmentRepository assignmentRepository;
    private final CourseAccessPort courseAccessPort;
    private final CourseContentPort contentPort;

    public AssignmentPermission(AssignmentRepository assignmentRepository,
                                CourseAccessPort courseAccessPort,
                                CourseContentPort contentPort) {
        this.assignmentRepository = assignmentRepository;
        this.courseAccessPort = courseAccessPort;
        this.contentPort = contentPort;
    }

    public static boolean isTeacher(String role) {
        return ROLE_TEACHER.equals(role);
    }

    public Assignment requireManagedAssignment(UUID teacherId, UUID assignmentId) {
        Assignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> notFound("Không tìm thấy bài tập"));
        requireManagedCourse(teacherId, assignment.getCourseId());
        return assignment;
    }

    public void requireManagedCourse(UUID teacherId, UUID courseId) {
        if (!courseAccessPort.isOwner(teacherId, courseId)) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Bạn không phụ trách lớp của khóa học này", HttpStatus.FORBIDDEN);
        }
    }

    /** Giáo viên phụ trách hoặc học sinh thuộc lớp; học sinh chỉ thấy bài đã xuất bản. */
    public Assignment requireVisibleAssignment(UUID actorId, String role, UUID assignmentId) {
        Assignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> notFound("Không tìm thấy bài tập"));

        if (isTeacher(role)) {
            requireManagedCourse(actorId, assignment.getCourseId());
            return assignment;
        }

        if (!courseAccessPort.isStudentEnrolled(actorId, assignment.getCourseId())) {
            throw new BusinessException("COURSE_ACCESS_DENIED",
                    "Bạn không có quyền truy cập khóa học này", HttpStatus.FORBIDDEN);
        }
        requirePublishedForStudent(assignment);
        return assignment;
    }

    /** AC-T03.4 + AC-S02.5: nháp hoặc thuộc chủ đề/bài học chưa xuất bản thì học sinh không thấy. */
    public void requirePublishedForStudent(Assignment assignment) {
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("ASSIGNMENT_NOT_PUBLISHED",
                    "Bài tập chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
        if (assignment.getTopicId() != null && !contentPort.isTopicPublished(assignment.getTopicId())) {
            throw new BusinessException("ASSIGNMENT_NOT_PUBLISHED",
                    "Chủ đề của bài tập chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
        if (assignment.getLessonId() != null && !contentPort.isLessonPublished(assignment.getLessonId())) {
            throw new BusinessException("ASSIGNMENT_NOT_PUBLISHED",
                    "Bài học của bài tập chưa được xuất bản", HttpStatus.FORBIDDEN);
        }
    }

    public BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
