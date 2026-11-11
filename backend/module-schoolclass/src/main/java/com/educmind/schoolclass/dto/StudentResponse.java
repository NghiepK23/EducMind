package com.educmind.schoolclass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentResponse(UUID id, String fullName, String email, OffsetDateTime joinedAt) {
}
