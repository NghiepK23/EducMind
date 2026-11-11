package com.educmind.analytics.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.CourseAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Quyền xem phân tích: giáo viên chỉ xem lớp mình phụ trách (AC-T07.5, AC-T08.6). */
@Component
public class AnalyticsPermission {

    private final ClassAccessPort classAccessPort;
    private final CourseAccessPort courseAccessPort;

    public AnalyticsPermission(ClassAccessPort classAccessPort, CourseAccessPort courseAccessPort) {
        this.classAccessPort = classAccessPort;
        this.courseAccessPort = courseAccessPort;
    }

    public void requireClassOwner(UUID teacherId, UUID classId) {
        if (!classAccessPort.isOwner(teacherId, classId)) {
            throw new BusinessException("NOT_CLASS_OWNER",
                    "Bạn không phụ trách lớp này", HttpStatus.FORBIDDEN);
        }
    }

    public void requireCourseOwner(UUID teacherId, UUID courseId) {
        if (!courseAccessPort.isOwner(teacherId, courseId)) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Bạn không phụ trách lớp của khóa học này", HttpStatus.FORBIDDEN);
        }
    }

    public void requireStudentInClass(UUID classId, UUID studentId) {
        if (!classAccessPort.isMember(studentId, classId)) {
            throw new BusinessException("STUDENT_NOT_IN_CLASS",
                    "Học sinh không thuộc lớp này", HttpStatus.FORBIDDEN);
        }
    }

    public BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
