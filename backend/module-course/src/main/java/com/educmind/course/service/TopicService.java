package com.educmind.course.service;

import com.educmind.course.dto.CreateTopicRequest;
import com.educmind.course.dto.TopicResponse;
import com.educmind.course.dto.UpdateTopicRequest;
import com.educmind.course.entity.ContentStatus;
import com.educmind.course.entity.CourseTopic;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.CourseTopicRepository;
import com.educmind.course.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * US-T02A — tạo và quản lý chủ đề; AC-T02A.2 chủ đề mới là DRAFT và học sinh không thấy;
 * AC-S01.4 học sinh không thấy chủ đề chưa xuất bản (kể cả bài học bên trong).
 */
@Service
public class TopicService {

    private final CourseTopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final CoursePermission permission;

    public TopicService(CourseTopicRepository topicRepository,
                        LessonRepository lessonRepository,
                        CoursePermission permission) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.permission = permission;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> list(UUID actorId, String role, UUID courseId) {
        permission.requireVisibleCourse(actorId, role, courseId);

        boolean teacher = CoursePermission.isTeacher(role);
        List<TopicResponse> result = new ArrayList<>();
        for (CourseTopic t : topicRepository.findByCourseIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(courseId)) {
            if (teacher || t.getStatus() == ContentStatus.PUBLISHED) {
                result.add(TopicResponse.from(t));
            }
        }
        return result;
    }

    @Transactional
    public TopicResponse create(UUID teacherId, UUID courseId, CreateTopicRequest request) {
        permission.requireManagedCourse(teacherId, courseId);
        CourseTopic topic = new CourseTopic(courseId, request.getTitle().trim(),
                request.getDescription(), request.getDisplayOrder());
        return TopicResponse.from(topicRepository.save(topic));
    }

    @Transactional
    public TopicResponse update(UUID teacherId, UUID topicId, UpdateTopicRequest request) {
        CourseTopic topic = permission.requireManagedTopic(teacherId, topicId);
        topic.update(request.getTitle() == null ? null : request.getTitle().trim(),
                request.getDescription(), request.getDisplayOrder(), teacherId);
        return TopicResponse.from(topicRepository.save(topic));
    }

    /** AC-T12.5: xóa mềm chủ đề; các bài học bên trong cũng bị ẩn, kết quả học tập vẫn giữ. */
    @Transactional
    public void delete(UUID teacherId, UUID topicId) {
        CourseTopic topic = permission.requireManagedTopic(teacherId, topicId);
        for (Lesson lesson : lessonRepository.findByTopicIdAndDeletedAtIsNullOrderByCreatedAtAsc(topicId)) {
            lesson.markDeleted(teacherId);
            lessonRepository.save(lesson);
        }
        topic.markDeleted(teacherId);
        topicRepository.save(topic);
    }

    @Transactional
    public TopicResponse setPublished(UUID teacherId, UUID topicId, boolean published) {
        CourseTopic topic = permission.requireManagedTopic(teacherId, topicId);
        if (published) {
            topic.publish(teacherId);
        } else {
            topic.unpublish(teacherId);
        }
        return TopicResponse.from(topicRepository.save(topic));
    }
}
