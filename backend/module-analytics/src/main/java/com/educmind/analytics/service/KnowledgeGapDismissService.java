package com.educmind.analytics.service;

import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Giáo viên bỏ qua (DISMISS) một knowledge gap — Module05 FINAL mục 4.4.
 *
 * Quyền: owner HOẶC đồng giảng dạy của course chứa gap (ngang quyền,
 * courseAccessPort.isOwnerOrCoTeacher). Không dùng isOwner (đó là ngoại lệ chấm điểm Module 4).
 *
 * Thứ tự kiểm tra: tồn tại (404) -> quyền (403) -> trạng thái (409). Kiểm quyền trước
 * trạng thái để người không có quyền không dò được trạng thái gap.
 */
@Service
public class KnowledgeGapDismissService {

    private final KnowledgeGapRepository gapRepository;
    private final CourseAccessPort courseAccessPort;

    public KnowledgeGapDismissService(KnowledgeGapRepository gapRepository,
                                      CourseAccessPort courseAccessPort) {
        this.gapRepository = gapRepository;
        this.courseAccessPort = courseAccessPort;
    }

    @Transactional
    public KnowledgeGap dismiss(UUID actorId, UUID gapId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "reason không được để trống",
                    HttpStatus.BAD_REQUEST);
        }
        KnowledgeGap gap = gapRepository.findById(gapId)
                .orElseThrow(() -> new BusinessException("GAP_NOT_FOUND",
                        "Knowledge gap không tồn tại", HttpStatus.NOT_FOUND));
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, gap.getCourseId())) {
            throw new BusinessException("FORBIDDEN_NOT_COURSE_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được dismiss knowledge gap",
                    HttpStatus.FORBIDDEN);
        }
        if (!gap.isOpen()) {
            throw new BusinessException("GAP_ALREADY_CLOSED",
                    "Knowledge gap đã đóng (" + gap.getStatus() + ")", HttpStatus.CONFLICT);
        }
        gap.dismiss(actorId, reason.trim());
        return gapRepository.save(gap);
    }
}
