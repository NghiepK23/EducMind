package com.educmind.course.dto;

import com.educmind.course.entity.Course;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CourseResponse(UUID id, UUID classId, String title, String description,
                             OffsetDateTime createdAt) {

    public static CourseResponse from(Course c) {
        return new CourseResponse(c.getId(), c.getClassId(), c.getTitle(), c.getDescription(), c.getCreatedAt());
    }
}
