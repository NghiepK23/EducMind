package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.course.entity.Course;
import com.educmind.course.entity.CourseTopic;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.CourseRepository;
import com.educmind.course.repository.CourseTopicRepository;
import com.educmind.course.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tập trung mọi kiểm tra quyền của module course.
 * Spec v2: quyền quản lý khóa học = quyền của giáo viên phụ trách lớp (classes.owner_teacher_id);
 * học sinh chỉ xem được khóa học của lớp mình.
 */
@Component
public class CoursePermission {

    public static final String ROLE_TEACHER = "TEACHER";

    private final CourseRepository courseRepository;
    private final CourseTopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final ClassAccessPort classAccessPort;

    public CoursePermission(CourseRepository courseRepository,
                            CourseTopicRepository topicRepository,
                            LessonRepository lessonRepository,
                            ClassAccessPort classAccessPort) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.classAccessPort = classAccessPort;
    }

    public static boolean isTeacher(String role) {
        return ROLE_TEACHER.equals(role);
    }

    /** Khóa học mà giáo viên được quản lý (phụ trách lớp của khóa học). */
    public Course requireManagedCourse(UUID teacherId, UUID courseId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> notFound("Không tìm thấy khóa học"));
        if (!classAccessPort.isOwner(teacherId, course.getClassId())) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Bạn không phụ trách lớp của khóa học này", HttpStatus.FORBIDDEN);
        }
        return course;
    }

    public CourseTopic requireManagedTopic(UUID teacherId, UUID topicId) {
        CourseTopic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                .orElseThrow(() -> notFound("Không tìm thấy chủ đề"));
        requireManagedCourse(teacherId, topic.getCourseId());
        return topic;
    }

    public Lesson requireManagedLesson(UUID teacherId, UUID lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .orElseThrow(() -> notFound("Không tìm thấy bài học"));
        requireManagedCourse(teacherId, lesson.getCourseId());
        return lesson;
    }

    /** Khóa học mà người dùng được XEM (giáo viên phụ trách hoặc học sinh của lớp). */
    public Course requireVisibleCourse(UUID actorId, String role, UUID courseId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> notFound("Không tìm thấy khóa học"));

        boolean allowed = isTeacher(role)
                ? classAccessPort.isOwner(actorId, course.getClassId())
                : classAccessPort.isMember(actorId, course.getClassId());

        if (!allowed) {
            throw new BusinessException("COURSE_ACCESS_DENIED",
                    "Bạn không có quyền truy cập khóa học này", HttpStatus.FORBIDDEN);
        }
        return course;
    }

    public BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
