package com.educmind.course.dto;

import com.educmind.course.entity.Lesson;

import java.util.UUID;

public record LessonResponse(UUID id, UUID courseId, UUID topicId, String title, String description,
                             String status, long objectiveCount) {

    public static LessonResponse from(Lesson l, long objectiveCount) {
        return new LessonResponse(l.getId(), l.getCourseId(), l.getTopicId(), l.getTitle(),
                l.getDescription(), l.getStatus().name(), objectiveCount);
    }
}
