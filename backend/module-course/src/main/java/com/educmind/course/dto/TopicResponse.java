package com.educmind.course.dto;

import com.educmind.course.entity.CourseTopic;

import java.util.UUID;

public record TopicResponse(UUID id, UUID courseId, String title, String description,
                            Integer displayOrder, String status) {

    public static TopicResponse from(CourseTopic t) {
        return new TopicResponse(t.getId(), t.getCourseId(), t.getTitle(), t.getDescription(),
                t.getDisplayOrder(), t.getStatus().name());
    }
}
