package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.course.dto.CreateLessonRequest;
import com.educmind.course.dto.LessonResponse;
import com.educmind.course.dto.UpdateLessonRequest;
import com.educmind.course.entity.ContentStatus;
import com.educmind.course.entity.CourseTopic;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.CourseTopicRepository;
import com.educmind.course.repository.LearningObjectiveRepository;
import com.educmind.course.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * US-T02 — bài học, mục tiêu học tập, tài liệu.
 * AC-T02.1 bài học mới là DRAFT; AC-T02.4 chỉ xuất bản khi có ít nhất 1 mục tiêu;
 * AC-T02.6 bài học thuộc một chủ đề của cùng khóa học; AC-S01.4 học sinh không thấy
 * bài học trong chủ đề chưa xuất bản.
 */
@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseTopicRepository topicRepository;
    private final LearningObjectiveRepository objectiveRepository;
    private final CoursePermission permission;

    public LessonService(LessonRepository lessonRepository,
                         CourseTopicRepository topicRepository,
                         LearningObjectiveRepository objectiveRepository,
                         CoursePermission permission) {
        this.lessonRepository = lessonRepository;
        this.topicRepository = topicRepository;
        this.objectiveRepository = objectiveRepository;
        this.permission = permission;
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> listByTopic(UUID actorId, String role, UUID topicId) {
        CourseTopic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy chủ đề"));

        permission.requireVisibleCourse(actorId, role, topic.getCourseId());

        boolean teacher = CoursePermission.isTeacher(role);
        if (!teacher && topic.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException("TOPIC_NOT_PUBLISHED",
                    "Chủ đề chưa được xuất bản", HttpStatus.FORBIDDEN);
        }

        List<LessonResponse> result = new ArrayList<>();
        for (Lesson l : lessonRepository.findByTopicIdAndDeletedAtIsNullOrderByCreatedAtAsc(topicId)) {
            if (teacher || l.getStatus() == ContentStatus.PUBLISHED) {
                result.add(LessonResponse.from(l, objectiveRepository.countByLessonId(l.getId())));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> listByCourse(UUID actorId, String role, UUID courseId) {
        permission.requireVisibleCourse(actorId, role, courseId);

        boolean teacher = CoursePermission.isTeacher(role);
        List<LessonResponse> result = new ArrayList<>();
        for (Lesson l : lessonRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(courseId)) {
            if (!teacher) {
                boolean topicPublished = topicRepository.findByIdAndDeletedAtIsNull(l.getTopicId())
                        .map(t -> t.getStatus() == ContentStatus.PUBLISHED)
                        .orElse(false);
                if (!topicPublished || l.getStatus() != ContentStatus.PUBLISHED) {
                    continue;
                }
            }
            result.add(LessonResponse.from(l, objectiveRepository.countByLessonId(l.getId())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public LessonResponse get(UUID actorId, String role, UUID lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài học"));
        permission.requireVisibleCourse(actorId, role, lesson.getCourseId());

        if (!CoursePermission.isTeacher(role)) {
            boolean visible = lesson.getStatus() == ContentStatus.PUBLISHED
                    && topicRepository.findByIdAndDeletedAtIsNull(lesson.getTopicId())
                        .map(t -> t.getStatus() == ContentStatus.PUBLISHED)
                        .orElse(false);
            if (!visible) {
                throw new BusinessException("LESSON_NOT_PUBLISHED",
                        "Bài học chưa được xuất bản", HttpStatus.FORBIDDEN);
            }
        }
        return LessonResponse.from(lesson, objectiveRepository.countByLessonId(lessonId));
    }

    @Transactional
    public LessonResponse create(UUID teacherId, UUID topicId, CreateLessonRequest request) {
        CourseTopic topic = permission.requireManagedTopic(teacherId, topicId);

        Lesson lesson = new Lesson(topic.getCourseId(), topicId,
                request.getTitle().trim(), request.getDescription());
        return LessonResponse.from(lessonRepository.save(lesson), 0);
    }

    @Transactional
    public LessonResponse update(UUID teacherId, UUID lessonId, UpdateLessonRequest request) {
        Lesson lesson = permission.requireManagedLesson(teacherId, lessonId);

        if (request.getTopicId() != null && !request.getTopicId().equals(lesson.getTopicId())) {
            CourseTopic target = topicRepository.findByIdAndDeletedAtIsNull(request.getTopicId())
                    .orElseThrow(() -> permission.notFound("Không tìm thấy chủ đề đích"));
            if (!target.getCourseId().equals(lesson.getCourseId())) {
                throw new BusinessException("TOPIC_NOT_IN_SAME_COURSE",
                        "Chủ đề phải thuộc cùng khóa học với bài học", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        lesson.update(request.getTitle() == null ? null : request.getTitle().trim(),
                request.getDescription(), request.getTopicId(), teacherId);

        return LessonResponse.from(lessonRepository.save(lesson),
                objectiveRepository.countByLessonId(lessonId));
    }

    /** AC-T02.5 / AC-T12.1: xóa mềm, kết quả học tập vẫn giữ. */
    @Transactional
    public void delete(UUID teacherId, UUID lessonId) {
        Lesson lesson = permission.requireManagedLesson(teacherId, lessonId);
        lesson.markDeleted(teacherId);
        lessonRepository.save(lesson);
    }

    /** AC-T02.4: bài học phải có ít nhất 1 mục tiêu học tập mới được xuất bản. */
    @Transactional
    public LessonResponse setPublished(UUID teacherId, UUID lessonId, boolean published) {
        Lesson lesson = permission.requireManagedLesson(teacherId, lessonId);

        if (published) {
            if (objectiveRepository.countByLessonId(lessonId) == 0) {
                throw new BusinessException("LESSON_HAS_NO_OBJECTIVE",
                        "Bài học phải có ít nhất một mục tiêu học tập trước khi xuất bản",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            lesson.publish(teacherId);
        } else {
            lesson.unpublish(teacherId);
        }
        return LessonResponse.from(lessonRepository.save(lesson),
                objectiveRepository.countByLessonId(lessonId));
    }
}
