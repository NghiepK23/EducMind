package com.educmind.course.service;

import com.educmind.common.port.CourseContentPort;
import com.educmind.course.entity.ContentStatus;
import com.educmind.course.entity.CourseTopic;
import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.CourseTopicRepository;
import com.educmind.course.repository.LearningObjectiveRepository;
import com.educmind.course.repository.LessonRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Triển khai {@link CourseContentPort} cho module bài tập / bài kiểm tra. */
@Component
public class CourseContentPortImpl implements CourseContentPort {

    private final CourseTopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LearningObjectiveRepository objectiveRepository;

    public CourseContentPortImpl(CourseTopicRepository topicRepository,
                                 LessonRepository lessonRepository,
                                 LearningObjectiveRepository objectiveRepository) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.objectiveRepository = objectiveRepository;
    }

    @Override
    public Optional<UUID> findTopicCourseId(UUID topicId) {
        return topicRepository.findByIdAndDeletedAtIsNull(topicId).map(CourseTopic::getCourseId);
    }

    @Override
    public Optional<UUID> findLessonCourseId(UUID lessonId) {
        return lessonRepository.findByIdAndDeletedAtIsNull(lessonId).map(Lesson::getCourseId);
    }

    @Override
    public boolean isTopicPublished(UUID topicId) {
        return topicRepository.findByIdAndDeletedAtIsNull(topicId)
                .map(t -> t.getStatus() == ContentStatus.PUBLISHED)
                .orElse(false);
    }

    @Override
    public boolean isLessonPublished(UUID lessonId) {
        Optional<Lesson> lesson = lessonRepository.findByIdAndDeletedAtIsNull(lessonId);
        if (lesson.isEmpty() || lesson.get().getStatus() != ContentStatus.PUBLISHED) {
            return false;
        }
        // AC-S01.4: chủ đề chưa xuất bản thì bài học bên trong cũng bị ẩn.
        return isTopicPublished(lesson.get().getTopicId());
    }

    @Override
    public List<ObjectiveRef> findObjectives(Collection<UUID> objectiveIds) {
        List<ObjectiveRef> result = new ArrayList<>();
        for (LearningObjective objective : objectiveRepository.findAllById(objectiveIds)) {
            lessonRepository.findByIdAndDeletedAtIsNull(objective.getLessonId())
                    .ifPresent(lesson -> result.add(new ObjectiveRef(objective.getId(), lesson.getCourseId())));
        }
        return result;
    }

    @Override
    public List<ObjectiveRef> findObjectivesByCourse(UUID courseId) {
        List<ObjectiveRef> result = new ArrayList<>();
        for (Lesson lesson : lessonRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(courseId)) {
            for (LearningObjective objective : objectiveRepository.findByLessonIdOrderByCodeAsc(lesson.getId())) {
                result.add(new ObjectiveRef(objective.getId(), courseId));
            }
        }
        return result;
    }
}
