package com.educmind.schoolclass.dto;

import com.educmind.schoolclass.entity.SchoolClass;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClassResponse(UUID id, String name, Short grade, String schoolYear,
                            UUID ownerTeacherId, int studentCount, OffsetDateTime createdAt) {

    public static ClassResponse from(SchoolClass c, int studentCount) {
        return new ClassResponse(c.getId(), c.getName(), c.getGrade(), c.getSchoolYear(),
                c.getOwnerTeacherId(), studentCount, c.getCreatedAt());
    }
}
