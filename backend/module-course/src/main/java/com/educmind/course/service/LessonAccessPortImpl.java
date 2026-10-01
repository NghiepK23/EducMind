package com.educmind.course.service;

import com.educmind.common.port.LessonAccessPort;
import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.LessonRepository;
import com.educmind.course.repository.ResourceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation của LessonAccessPort (định nghĩa ở educmind-common).
 * Adapter duy nhất được đọc lessons.learning_objectives (JSONB) cho các module khác.
 */
@Component
public class LessonAccessPortImpl implements LessonAccessPort {

    private final LessonRepository lessonRepository;
    private final ResourceRepository resourceRepository;

    public LessonAccessPortImpl(LessonRepository lessonRepository, ResourceRepository resourceRepository) {
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectiveInfo> findObjectivesByLessonId(UUID lessonId) {
        return lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .map(this::toObjectiveInfos)
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean objectiveBelongsToLesson(UUID lessonId, UUID objectiveId) {
        if (lessonId == null || objectiveId == null) {
            return false;
        }
        return findObjectivesByLessonId(lessonId).stream()
                .anyMatch(o -> objectiveId.equals(o.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean lessonBelongsToCourse(UUID lessonId, UUID courseId) {
        if (lessonId == null || courseId == null) {
            return false;
        }
        return lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .map(lesson -> courseId.equals(lesson.getCourseId()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findLatestResourceIdByLessonId(UUID lessonId) {
        if (lessonId == null) {
            return Optional.empty();
        }
        return resourceRepository.findFirstByLessonIdAndDeletedAtIsNullOrderByCreatedAtDesc(lessonId)
                .map(r -> r.getId());
    }

    private List<ObjectiveInfo> toObjectiveInfos(Lesson lesson) {
        List<LearningObjective> objectives = lesson.getLearningObjectives();
        if (objectives == null) {
            return List.of();
        }
        return objectives.stream()
                .filter(o -> o.getId() != null)
                .map(o -> new ObjectiveInfo(o.getId(), o.getText()))
                .toList();
    }
}
