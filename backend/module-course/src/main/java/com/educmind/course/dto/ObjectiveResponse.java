package com.educmind.course.dto;

import com.educmind.course.entity.LearningObjective;

import java.util.UUID;

public record ObjectiveResponse(UUID id, UUID lessonId, String code, String description) {

    public static ObjectiveResponse from(LearningObjective o) {
        return new ObjectiveResponse(o.getId(), o.getLessonId(), o.getCode(), o.getDescription());
    }
}
