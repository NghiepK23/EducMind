package com.educmind.auth.service;

import com.educmind.auth.dto.CreateInviteCodeRequest;
import com.educmind.auth.dto.InviteCodeResponse;
import com.educmind.auth.entity.InviteCode;
import com.educmind.auth.entity.Role;
import com.educmind.auth.repository.InviteCodeRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logic nghiệp vụ cho invite-code CRUD — Module01_Auth_SchoolClass_FINAL.md, mục A.5.3 + A.6.
 *
 * Quyền hạn (A.5.3): "Chỉ TEACHER là owner_teacher_id hoặc thành viên giảng dạy của class_id
 * mới được tạo/thu hồi invite code cho lớp đó" — dùng chung ClassAccessPort.isOwnerOrCoTeacher()
 * cho cả 3 endpoint (create/list/revoke), không tự viết logic check quyền riêng ở đây.
 */
@Service
public class InviteCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ ký tự dễ nhầm: I, O, 0, 1

    private final InviteCodeRepository inviteCodeRepository;
    private final ClassAccessPort classAccessPort;

    public InviteCodeService(InviteCodeRepository inviteCodeRepository, ClassAccessPort classAccessPort) {
        this.inviteCodeRepository = inviteCodeRepository;
        this.classAccessPort = classAccessPort;
    }

    @Transactional
    public InviteCodeResponse createInviteCode(UUID teacherId, CreateInviteCodeRequest request) {
        UUID schoolId = classAccessPort.findSchoolIdByClassId(request.getClassId())
                .orElseThrow(() -> new BusinessException(
                        "CLASS_NOT_FOUND",
                        "Lớp không tồn tại",
                        HttpStatus.NOT_FOUND
                ));

        if (!classAccessPort.isOwnerOrCoTeacher(teacherId, request.getClassId())) {
            throw new BusinessException(
                    "NOT_CLASS_TEACHER",
                    "Bạn không phải giáo viên phụ trách hoặc đồng giảng dạy lớp này",
                    HttpStatus.FORBIDDEN
            );
        }

        String code = generateUniqueCode();
        InviteCode inviteCode = new InviteCode(
                code, request.getClassId(), schoolId, Role.STUDENT, request.getMaxUses(), teacherId
        );
        inviteCode.setExpiresAt(request.getExpiresAt());
        inviteCode = inviteCodeRepository.save(inviteCode);

        return toResponse(inviteCode);
    }

    /**
     * GET /api/v1/invite-codes — danh sách invite code do chính teacherId tạo ra.
     *
     * [Giải thích phạm vi] Spec A.6 ghi "xem code của lớp mình" — hiểu theo nghĩa thực tế
     * là danh sách code MÀ GIÁO VIÊN NÀY ĐÃ TẠO (created_by = teacherId), không phải mọi
     * code của MỌI lớp giáo viên này có quyền (kể cả lớp không phải do chính họ tạo code).
     * InviteCodeRepository hiện chỉ có findByClassId, không có findByCreatedBy — cần thêm.
     */
    @Transactional(readOnly = true)
    public List<InviteCodeResponse> listInviteCodes(UUID teacherId) {
        return inviteCodeRepository.findByCreatedBy(teacherId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeInviteCode(UUID teacherId, UUID inviteCodeId) {
        InviteCode inviteCode = inviteCodeRepository.findById(inviteCodeId)
                .orElseThrow(() -> new BusinessException(
                        "INVITE_CODE_NOT_FOUND",
                        "Invite code không tồn tại",
                        HttpStatus.NOT_FOUND
                ));

        if (!classAccessPort.isOwnerOrCoTeacher(teacherId, inviteCode.getClassId())) {
            throw new BusinessException(
                    "NOT_CLASS_TEACHER",
                    "Bạn không phải giáo viên phụ trách hoặc đồng giảng dạy lớp này",
                    HttpStatus.FORBIDDEN
            );
        }

        if (inviteCode.getRevokedAt() != null) {
            throw new BusinessException(
                    "INVITE_CODE_ALREADY_REVOKED",
                    "Invite code đã bị thu hồi trước đó",
                    HttpStatus.CONFLICT
            );
        }

        inviteCode.setRevokedAt(OffsetDateTime.now());
        inviteCodeRepository.save(inviteCode);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (inviteCodeRepository.findByCode(code).isPresent());
        return code;
    }

    private InviteCodeResponse toResponse(InviteCode inviteCode) {
        return new InviteCodeResponse(
                inviteCode.getId(),
                inviteCode.getCode(),
                inviteCode.getClassId(),
                inviteCode.getIntendedRole(),
                inviteCode.getMaxUses(),
                inviteCode.getUsedCount(),
                inviteCode.getExpiresAt(),
                inviteCode.getCreatedAt(),
                inviteCode.getRevokedAt()
        );
    }
}
