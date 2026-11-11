package com.educmind.analytics.dto;

import java.util.List;
import java.util.UUID;

public record StudentMasteryRow(UUID studentId, String fullName, String email,
                                List<ObjectiveMasteryRow> objectives) {
}
