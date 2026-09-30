package com.educmind.schoolclass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response dùng chung cho add/remove member — Module01_Auth_SchoolClass_FINAL.md, mục B.6.
 * leftAt: null = đang là thành viên active; có giá trị = đã rời (dùng cho response
 * của DELETE /members/{userId} sau này).
 */
public record MemberResponse(
        UUID id,
        UUID classId,
        UUID userId,
        String memberRole,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt
) {}
