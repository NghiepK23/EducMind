package com.educmind.reports.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.AiUsagePort;
import com.educmind.common.port.AiUsagePort.AiUsageSummary;
import com.educmind.common.port.UserLookupPort;
import com.educmind.common.port.UserLookupPort.UserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Báo cáo mức dùng AI cấp trường (PATCH 3 + PATCH2 C.5): gồm nhóm quality và operational.
 * SCHOOL_ADMIN chỉ xem trường của mình; SUPER_ADMIN xem mọi trường. Không trả nội dung content.
 */
@Service
public class AiUsageReportService {

    private static final int DEFAULT_DAYS = 30;

    private final AiUsagePort aiUsagePort;
    private final UserLookupPort userLookupPort;

    public AiUsageReportService(AiUsagePort aiUsagePort, UserLookupPort userLookupPort) {
        this.aiUsagePort = aiUsagePort;
        this.userLookupPort = userLookupPort;
    }

    public AiUsageSummary generate(UUID actorId, UUID schoolId,
                                   OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        checkAccess(actorId, schoolId);
        OffsetDateTime end = periodEnd != null ? periodEnd : OffsetDateTime.now();
        OffsetDateTime start = periodStart != null ? periodStart : end.minusDays(DEFAULT_DAYS);
        if (!start.isBefore(end)) {
            throw new BusinessException("INVALID_PERIOD_RANGE",
                    "period_start phải trước period_end", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return aiUsagePort.summarizeBySchool(schoolId, start, end);
    }

    public void checkAccess(UUID actorId, UUID schoolId) {
        UserSummary actor = userLookupPort.findUserSummary(actorId).orElseThrow(() ->
                new BusinessException("REPORT_SCOPE_FORBIDDEN",
                        "Không xác định được người dùng", HttpStatus.FORBIDDEN));
        if ("SUPER_ADMIN".equals(actor.role())) {
            return;
        }
        if (!"SCHOOL_ADMIN".equals(actor.role()) || !schoolId.equals(actor.schoolId())) {
            throw new BusinessException("REPORT_SCOPE_FORBIDDEN",
                    "Chỉ xem được báo cáo của trường mình", HttpStatus.FORBIDDEN);
        }
    }
}
