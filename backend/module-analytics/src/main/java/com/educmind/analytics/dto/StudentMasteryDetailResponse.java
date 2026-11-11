package com.educmind.analytics.dto;

import java.util.List;
import java.util.UUID;

public record StudentMasteryDetailResponse(UUID studentId, String fullName, String email,
                                           List<ObjectiveDetailRow> objectives) {
}
