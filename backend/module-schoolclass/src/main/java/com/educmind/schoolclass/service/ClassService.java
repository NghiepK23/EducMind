package com.educmind.schoolclass.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.UserLookupPort;
import com.educmind.schoolclass.dto.AddMemberRequest;
import com.educmind.schoolclass.dto.ClassResponse;
import com.educmind.schoolclass.dto.CreateClassRequest;
import com.educmind.schoolclass.dto.MemberResponse;
import com.educmind.schoolclass.dto.UpdateClassRequest;
import com.educmind.schoolclass.entity.ClassMember;
import com.educmind.schoolclass.entity.MemberRole;
import com.educmind.schoolclass.entity.SchoolClass;
import com.educmind.schoolclass.repository.ClassMemberRepository;
import com.educmind.schoolclass.repository.SchoolClassRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logic nghiệp vụ Class CRUD — Module01_Auth_SchoolClass_FINAL.md, Phần B.
 */
@Service
public class ClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserLookupPort userLookupPort;

    public ClassService(SchoolClassRepository schoolClassRepository,
                         ClassMemberRepository classMemberRepository,
                         UserLookupPort userLookupPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.classMemberRepository = classMemberRepository;
        this.userLookupPort = userLookupPort;
    }

    @Transactional
    public ClassResponse createClass(UUID actorId, CreateClassRequest request) {
        UserLookupPort.UserSummary actor = loadActor(actorId);

        UUID resolvedOwnerTeacherId;

        if ("TEACHER".equals(actor.role())) {
            resolvedOwnerTeacherId = actorId;
        } else {
            if (request.getOwnerTeacherId() == null) {
                throw new BusinessException(
                        "VALIDATION_ERROR",
                        "ownerTeacherId không được để trống khi tạo lớp với vai trò SCHOOL_ADMIN",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            resolvedOwnerTeacherId = validateOwnerTeacher(request.getOwnerTeacherId(), actor.schoolId());
        }

        SchoolClass schoolClass = new SchoolClass(actor.schoolId(), request.getName(), resolvedOwnerTeacherId, actorId);
        schoolClass.setGradeLevel(request.getGradeLevel());
        schoolClass.setAcademicYear(request.getAcademicYear());
        schoolClass = schoolClassRepository.save(schoolClass);

        return toResponse(schoolClass);
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(UUID actorId, UUID teacherIdFilter) {
        UserLookupPort.UserSummary actor = loadActor(actorId);

        List<SchoolClass> classes;
        if ("TEACHER".equals(actor.role())) {
            classes = schoolClassRepository.findAccessibleByTeacher(actorId);
        } else {
            classes = (teacherIdFilter != null)
                    ? schoolClassRepository.findBySchoolIdAndOwnerTeacherIdAndDeletedAtIsNull(actor.schoolId(), teacherIdFilter)
                    : schoolClassRepository.findBySchoolIdAndDeletedAtIsNull(actor.schoolId());
        }

        return classes.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClassResponse getClassDetail(UUID actorId, UUID classId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        SchoolClass schoolClass = loadClassOrThrow(classId);
        assertCanView(actor, schoolClass);
        return toResponse(schoolClass);
    }

    @Transactional
    public ClassResponse updateClass(UUID actorId, UUID classId, UpdateClassRequest request) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        SchoolClass schoolClass = loadClassOrThrow(classId);
        assertCanManage(actor, schoolClass);

        if (request.getName() != null) {
            if (!StringUtils.hasText(request.getName())) {
                throw new BusinessException(
                        "VALIDATION_ERROR",
                        "name không được để trống",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            schoolClass.setName(request.getName());
        }
        if (request.getGradeLevel() != null) {
            schoolClass.setGradeLevel(request.getGradeLevel());
        }
        if (request.getAcademicYear() != null) {
            schoolClass.setAcademicYear(request.getAcademicYear());
        }

        schoolClass = schoolClassRepository.saveAndFlush(schoolClass);
        return toResponse(schoolClass);
    }

    @Transactional
    public ClassResponse deleteClass(UUID actorId, UUID classId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        SchoolClass schoolClass = loadClassOrThrow(classId);
        assertCanManage(actor, schoolClass);

        schoolClass.setDeletedAt(OffsetDateTime.now());
        schoolClass = schoolClassRepository.saveAndFlush(schoolClass);

        return toResponse(schoolClass);
    }

    @Transactional
    public MemberResponse addMember(UUID actorId, UUID classId, AddMemberRequest request) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        SchoolClass schoolClass = loadClassOrThrow(classId);
        assertCanManage(actor, schoolClass);

        UserLookupPort.UserSummary targetUser = userLookupPort.findUserSummary(request.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_USER_NOT_FOUND",
                        "Không tìm thấy tài khoản người dùng được chỉ định",
                        HttpStatus.NOT_FOUND
                ));

        if (!targetUser.active()) {
            throw new BusinessException(
                    "MEMBER_USER_INACTIVE",
                    "Tài khoản người dùng được chỉ định hiện không hoạt động",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        if (!schoolClass.getSchoolId().equals(targetUser.schoolId())) {
            throw new BusinessException(
                    "MEMBER_DIFFERENT_SCHOOL",
                    "Không thể thêm người dùng không thuộc cùng trường vào lớp",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!targetUser.role().equals(request.getMemberRole())) {
            throw new BusinessException(
                    "MEMBER_ROLE_MISMATCH",
                    "memberRole không khớp với vai trò thật của người dùng được chỉ định",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        if (schoolClass.getOwnerTeacherId().equals(targetUser.id())) {
            throw new BusinessException(
                    "MEMBER_IS_OWNER",
                    "Giáo viên chủ nhiệm đã mặc định có quyền quản lý lớp, không cần thêm vào danh sách thành viên",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        MemberRole newRole = MemberRole.valueOf(request.getMemberRole());

        ClassMember member = classMemberRepository.findByClassIdAndUserId(classId, targetUser.id())
                .map(existing -> {
                    if (existing.getLeftAt() == null) {
                        throw new BusinessException(
                                "MEMBER_ALREADY_EXISTS",
                                "Người dùng này đã là thành viên của lớp",
                                HttpStatus.CONFLICT
                        );
                    }
                    existing.setLeftAt(null);
                    existing.setMemberRole(newRole);
                    return existing;
                })
                .orElseGet(() -> new ClassMember(classId, targetUser.id(), newRole));

        member = classMemberRepository.saveAndFlush(member);

        return toMemberResponse(member);
    }

    /**
     * DELETE /api/v1/classes/{id}/members/{userId} — Module01_Auth_SchoolClass_FINAL.md, mục B.6.
     * Quyền: assertCanManage, nhất quán với addMember.
     * "Xóa" ở đây nghĩa là set leftAt = now() (soft-remove) — KHÔNG xóa cứng dòng
     * class_members, giữ nguyên joinedAt/memberRole cũ để giữ lịch sử.
     * Không tìm thấy dòng active nào cho (classId, userId) — dù chưa từng là thành viên
     * hay đã rời từ trước — đều coi là "không có gì để xóa" → MEMBER_NOT_FOUND (404),
     * không phân biệt 2 case này thành 2 mã lỗi riêng.
     * Hệ quả tự nhiên: cố gỡ chính owner_teacher_id ra khỏi members cũng rơi vào
     * MEMBER_NOT_FOUND, vì owner chưa từng có dòng class_members (đã chặn ở addMember)
     * — không cần logic đặc biệt riêng cho trường hợp này.
     */
    @Transactional
    public MemberResponse removeMember(UUID actorId, UUID classId, UUID targetUserId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        SchoolClass schoolClass = loadClassOrThrow(classId);
        assertCanManage(actor, schoolClass);

        ClassMember member = classMemberRepository.findByClassIdAndUserId(classId, targetUserId)
                .filter(m -> m.getLeftAt() == null)
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "Không tìm thấy thành viên đang active với userId này trong lớp",
                        HttpStatus.NOT_FOUND
                ));

        member.setLeftAt(OffsetDateTime.now());
        member = classMemberRepository.saveAndFlush(member);

        return toMemberResponse(member);
    }

    private void assertCanView(UserLookupPort.UserSummary actor, SchoolClass schoolClass) {
        if ("SCHOOL_ADMIN".equals(actor.role())) {
            if (!schoolClass.getSchoolId().equals(actor.schoolId())) {
                throw forbidden();
            }
            return;
        }
        if ("TEACHER".equals(actor.role())) {
            boolean isOwner = schoolClass.getOwnerTeacherId().equals(actor.id());
            boolean isMember = classMemberRepository.findByClassIdAndUserId(schoolClass.getId(), actor.id()).isPresent();
            if (!isOwner && !isMember) {
                throw forbidden();
            }
            return;
        }
        if ("STUDENT".equals(actor.role())) {
            boolean isActiveMember = classMemberRepository
                    .findByClassIdAndUserId(schoolClass.getId(), actor.id())
                    .map(m -> m.getLeftAt() == null)
                    .orElse(false);
            if (!isActiveMember) {
                throw forbidden();
            }
            return;
        }
        throw forbidden();
    }

    private void assertCanManage(UserLookupPort.UserSummary actor, SchoolClass schoolClass) {
        if ("SCHOOL_ADMIN".equals(actor.role())) {
            if (!schoolClass.getSchoolId().equals(actor.schoolId())) {
                throw forbidden();
            }
            return;
        }
        if ("TEACHER".equals(actor.role())) {
            if (!schoolClass.getOwnerTeacherId().equals(actor.id())) {
                throw forbidden();
            }
            return;
        }
        throw forbidden();
    }

    private BusinessException forbidden() {
        return new BusinessException(
                "CLASS_ACCESS_FORBIDDEN",
                "Bạn không có quyền truy cập lớp học này",
                HttpStatus.FORBIDDEN
        );
    }

    private SchoolClass loadClassOrThrow(UUID classId) {
        return schoolClassRepository.findByIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new BusinessException(
                        "CLASS_NOT_FOUND",
                        "Không tìm thấy lớp học",
                        HttpStatus.NOT_FOUND
                ));
    }

    private UserLookupPort.UserSummary loadActor(UUID actorId) {
        return userLookupPort.findUserSummary(actorId)
                .orElseThrow(() -> new BusinessException(
                        "ACTOR_NOT_FOUND",
                        "Không tìm thấy thông tin tài khoản của bạn",
                        HttpStatus.UNAUTHORIZED
                ));
    }

    private UUID validateOwnerTeacher(UUID ownerTeacherId, UUID actorSchoolId) {
        UserLookupPort.UserSummary summary = userLookupPort.findUserSummary(ownerTeacherId)
                .orElseThrow(() -> new BusinessException(
                        "OWNER_TEACHER_NOT_FOUND",
                        "Giáo viên chủ nhiệm được chỉ định không tồn tại",
                        HttpStatus.NOT_FOUND
                ));

        if (!"TEACHER".equals(summary.role())) {
            throw new BusinessException(
                    "OWNER_TEACHER_INVALID_ROLE",
                    "Người dùng được chỉ định không phải giáo viên",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        if (!actorSchoolId.equals(summary.schoolId())) {
            throw new BusinessException(
                    "OWNER_TEACHER_DIFFERENT_SCHOOL",
                    "Giáo viên được chỉ định không thuộc trường của bạn",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!summary.active()) {
            throw new BusinessException(
                    "OWNER_TEACHER_INACTIVE",
                    "Giáo viên được chỉ định hiện không hoạt động",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        return summary.id();
    }

    private ClassResponse toResponse(SchoolClass schoolClass) {
        return new ClassResponse(
                schoolClass.getId(),
                schoolClass.getSchoolId(),
                schoolClass.getName(),
                schoolClass.getGradeLevel(),
                schoolClass.getAcademicYear(),
                schoolClass.getOwnerTeacherId(),
                schoolClass.getStatus(),
                schoolClass.getCreatedAt(),
                schoolClass.getUpdatedAt(),
                schoolClass.getDeletedAt()
        );
    }

    private MemberResponse toMemberResponse(ClassMember member) {
        return new MemberResponse(
                member.getId(),
                member.getClassId(),
                member.getUserId(),
                member.getMemberRole().name(),
                member.getJoinedAt(),
                member.getLeftAt()
        );
    }
}
