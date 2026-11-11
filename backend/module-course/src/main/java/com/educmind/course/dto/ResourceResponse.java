package com.educmind.course.dto;

import com.educmind.course.entity.Resource;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourceResponse(UUID id, UUID courseId, UUID lessonId, String fileName,
                               Long fileSizeBytes, String fileType, OffsetDateTime uploadedAt,
                               String downloadUrl) {

    public static ResourceResponse from(Resource r) {
        return new ResourceResponse(r.getId(), r.getCourseId(), r.getLessonId(), r.getFileName(),
                r.getFileSizeBytes(), r.getFileType(), r.getUploadedAt(),
                "/api/v1/resources/" + r.getId() + "/download");
    }
}
