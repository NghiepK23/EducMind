package com.educmind.analytics.dto;

import java.util.List;
import java.util.UUID;

public record ClassMasteryResponse(UUID classId, List<StudentMasteryRow> students,
                                   List<ObjectiveSummaryRow> objectiveSummary) {
}
