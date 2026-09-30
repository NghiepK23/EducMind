package com.educmind.course.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response dùng chung cho add/list members — thay thế hoàn toàn CourseClassResponse cũ. */
public class CourseMemberResponse {

    private final UUID id;
    private final UUID courseId;
    private final UUID userId;
    private final String memberRole;
    private final UUID sourceClassId;
    private final UUID addedBy;
    private final OffsetDateTime addedAt;
    private final OffsetDateTime leftAt;

    public CourseMemberResponse(UUID id, UUID courseId, UUID userId, String memberRole,
                                 UUID sourceClassId, UUID addedBy, OffsetDateTime addedAt,
                                 OffsetDateTime leftAt) {
        this.id = id;
        this.courseId = courseId;
        this.userId = userId;
        this.memberRole = memberRole;
        this.sourceClassId = sourceClassId;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
        this.leftAt = leftAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public UUID getSourceClassId() {
        return sourceClassId;
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

    public OffsetDateTime getLeftAt() {
        return leftAt;
    }
}
