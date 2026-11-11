package com.educmind.analytics.service;

import com.educmind.analytics.dto.KnowledgeGapRow;
import com.educmind.analytics.entity.GapStatus;
import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.entity.Mastery;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.CourseContentPort;
import com.educmind.common.port.UserLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** US-T08 — nội dung học sinh cần cải thiện: xem danh sách, lọc, bỏ qua kèm lý do. */
@Service
public class KnowledgeGapService {

    private final KnowledgeGapRepository gapRepository;
    private final MasteryRepository masteryRepository;
    private final AnalyticsQueryService queryService;
    private final AnalyticsPermission permission;
    private final ClassAccessPort classAccessPort;
    private final CourseContentPort contentPort;
    private final UserLookupPort userLookupPort;

    public KnowledgeGapService(KnowledgeGapRepository gapRepository,
                               MasteryRepository masteryRepository,
                               AnalyticsQueryService queryService,
                               AnalyticsPermission permission,
                               ClassAccessPort classAccessPort,
                               CourseContentPort contentPort,
                               UserLookupPort userLookupPort) {
        this.gapRepository = gapRepository;
        this.masteryRepository = masteryRepository;
        this.queryService = queryService;
        this.permission = permission;
        this.classAccessPort = classAccessPort;
        this.contentPort = contentPort;
        this.userLookupPort = userLookupPort;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeGapRow> list(UUID teacherId, UUID classId, String status, UUID topicId, UUID lessonId) {
        permission.requireClassOwner(teacherId, classId);
        List<UUID> studentIds = classAccessPort.findStudentIds(classId);
        if (studentIds.isEmpty()) {
            return List.of();
        }

        // Mặc định chỉ hiển thị nội dung đang cần cải thiện (AC-T08.3);
        // ?status=RESOLVED / DISMISSED để xem lại lịch sử (AC-T08.6).
        GapStatus filter = parseStatus(status);
        List<KnowledgeGap> gaps = gapRepository.findByStudentIdInAndStatusOrderByOpenedAtDesc(
                studentIds, filter == null ? GapStatus.OPEN : filter);

        Map<UUID, CourseContentPort.ObjectiveInfo> infos = new HashMap<>();
        for (CourseContentPort.ObjectiveInfo info : queryService.classObjectives(teacherId, classId, topicId, lessonId)) {
            infos.put(info.id(), info);
        }
        Map<String, Mastery> mastery = queryService.masteryIndex(studentIds);

        Map<UUID, String> names = new HashMap<>();
        for (UserLookupPort.UserDetail detail : userLookupPort.findUserDetails(studentIds)) {
            names.put(detail.id(), detail.fullName());
        }

        List<KnowledgeGapRow> result = new ArrayList<>();
        for (KnowledgeGap gap : gaps) {
            CourseContentPort.ObjectiveInfo info = infos.get(gap.getObjectiveId());
            if (info == null) {
                // lọc theo chủ đề/bài học hoặc mục tiêu không còn trong lớp
                continue;
            }
            Mastery m = mastery.get(gap.getStudentId() + ":" + gap.getObjectiveId());
            result.add(toRow(gap, info, m == null ? null : m.getMasteryScore(),
                    names.getOrDefault(gap.getStudentId(), "")));
        }
        return result;
    }

    /** AC-T08.4: bỏ qua nội dung cần cải thiện và ghi lại lý do. */
    @Transactional
    public KnowledgeGapRow dismiss(UUID teacherId, UUID gapId, String reason) {
        KnowledgeGap gap = gapRepository.findById(gapId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy nội dung cần cải thiện"));

        CourseContentPort.ObjectiveInfo info = contentPort.findObjectiveInfos(List.of(gap.getObjectiveId()))
                .stream().findFirst()
                .orElseThrow(() -> permission.notFound("Không tìm thấy mục tiêu học tập"));
        permission.requireCourseOwner(teacherId, info.courseId());

        gap.dismiss(teacherId, OffsetDateTime.now(), reason.trim());
        gapRepository.save(gap);

        Mastery mastery = masteryRepository
                .findByStudentIdAndObjectiveId(gap.getStudentId(), gap.getObjectiveId()).orElse(null);
        String studentName = userLookupPort.findUserDetails(List.of(gap.getStudentId())).stream()
                .findFirst().map(UserLookupPort.UserDetail::fullName).orElse("");
        return toRow(gap, info, mastery == null ? null : mastery.getMasteryScore(), studentName);
    }

    private KnowledgeGapRow toRow(KnowledgeGap gap, CourseContentPort.ObjectiveInfo info,
                                  java.math.BigDecimal masteryScore, String studentName) {
        return new KnowledgeGapRow(gap.getId(), gap.getStudentId(), studentName,
                info.id(), info.code(), info.description(), info.lessonId(), info.lessonTitle(),
                info.topicId(), info.topicTitle(), gap.getStatus().name(), gap.getReason(), masteryScore,
                gap.getOpenedAt(), gap.getResolvedAt(), gap.getDismissedAt(), gap.getDismissReason());
    }

    private GapStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GapStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new BusinessException("INVALID_FILTER",
                    "Trạng thái không hợp lệ: " + value, HttpStatus.BAD_REQUEST);
        }
    }
}
