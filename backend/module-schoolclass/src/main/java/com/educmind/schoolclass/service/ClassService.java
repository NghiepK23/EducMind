package com.educmind.schoolclass.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.AccountPort;
import com.educmind.common.port.UserLookupPort;
import com.educmind.schoolclass.dto.AddStudentRequest;
import com.educmind.schoolclass.dto.ClassResponse;
import com.educmind.schoolclass.dto.CreateClassRequest;
import com.educmind.schoolclass.dto.StudentResponse;
import com.educmind.schoolclass.dto.UpdateClassRequest;
import com.educmind.schoolclass.entity.ClassMember;
import com.educmind.schoolclass.entity.SchoolClass;
import com.educmind.schoolclass.repository.ClassMemberRepository;
import com.educmind.schoolclass.repository.SchoolClassRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nghiệp vụ lớp học — Spec v2: US-T00 (tạo lớp, thêm/bớt học sinh).
 * Chỉ giáo viên phụ trách lớp mới được thao tác trên lớp đó.
 */
@Service
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final ClassMemberRepository memberRepository;
    private final AccountPort accountPort;
    private final UserLookupPort userLookupPort;

    public ClassService(SchoolClassRepository classRepository,
                        ClassMemberRepository memberRepository,
                        AccountPort accountPort,
                        UserLookupPort userLookupPort) {
        this.classRepository = classRepository;
        this.memberRepository = memberRepository;
        this.accountPort = accountPort;
        this.userLookupPort = userLookupPort;
    }

    /** AC-T00.1: người tạo lớp là giáo viên phụ trách. */
    @Transactional
    public ClassResponse create(UUID teacherId, CreateClassRequest request) {
        SchoolClass saved = classRepository.save(new SchoolClass(
                request.getName().trim(),
                request.getGrade(),
                request.getSchoolYear(),
                teacherId
        ));
        return ClassResponse.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> listForTeacher(UUID teacherId) {
        List<ClassResponse> result = new ArrayList<>();
        for (SchoolClass c : classRepository.findByOwnerTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(teacherId)) {
            result.add(ClassResponse.from(c, memberRepository.findByClassIdOrderByJoinedAtAsc(c.getId()).size()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ClassResponse get(UUID teacherId, UUID classId) {
        SchoolClass c = requireOwned(teacherId, classId);
        return ClassResponse.from(c, memberRepository.findByClassIdOrderByJoinedAtAsc(classId).size());
    }

    @Transactional
    public ClassResponse update(UUID teacherId, UUID classId, UpdateClassRequest request) {
        SchoolClass c = requireOwned(teacherId, classId);
        if (request.getName() != null) {
            c.rename(request.getName().trim());
        }
        if (request.getGrade() != null) {
            c.setGrade(request.getGrade());
        }
        if (request.getSchoolYear() != null) {
            c.setSchoolYear(request.getSchoolYear());
        }
        c.setUpdatedBy(teacherId);
        SchoolClass saved = classRepository.save(c);
        return ClassResponse.from(saved, memberRepository.findByClassIdOrderByJoinedAtAsc(classId).size());
    }

    /** Xóa mềm: kết quả học tập đã ghi nhận vẫn được giữ (US-T00.4). */
    @Transactional
    public void delete(UUID teacherId, UUID classId) {
        SchoolClass c = requireOwned(teacherId, classId);
        c.setUpdatedBy(teacherId);
        c.markDeleted();
        classRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(UUID teacherId, UUID classId) {
        requireOwned(teacherId, classId);
        List<ClassMember> members = memberRepository.findByClassIdOrderByJoinedAtAsc(classId);
        if (members.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = members.stream().map(ClassMember::getStudentId).toList();
        Map<UUID, UserLookupPort.UserDetail> byId = new HashMap<>();
        for (UserLookupPort.UserDetail d : userLookupPort.findUserDetails(ids)) {
            byId.put(d.id(), d);
        }

        List<StudentResponse> result = new ArrayList<>();
        for (ClassMember m : members) {
            UserLookupPort.UserDetail d = byId.get(m.getStudentId());
            if (d != null) {
                result.add(new StudentResponse(d.id(), d.fullName(), d.email(), m.getJoinedAt()));
            }
        }
        return result;
    }

    /**
     * US-T00/AC-T00.2: thêm học sinh bằng email. Chưa có tài khoản thì tạo mới + gửi link
     * đặt mật khẩu. AC-T00.3: không nhận email của tài khoản giáo viên, không trùng lặp.
     */
    @Transactional
    public StudentResponse addStudent(UUID teacherId, UUID classId, AddStudentRequest request) {
        requireOwned(teacherId, classId);

        String email = request.getEmail().trim();
        UUID studentId;
        UserLookupPort.UserDetail detail;

        var existingId = accountPort.findUserIdByEmail(email);
        if (existingId.isPresent()) {
            studentId = existingId.get();
            var summary = userLookupPort.findUserSummary(studentId)
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                            "Không tìm thấy tài khoản", HttpStatus.NOT_FOUND));

            if (!"STUDENT".equals(summary.role())) {
                throw new BusinessException("EMAIL_BELONGS_TO_TEACHER",
                        "Email này thuộc tài khoản giáo viên, không thể thêm vào lớp với vai trò học sinh",
                        HttpStatus.CONFLICT);
            }
            if (memberRepository.existsByClassIdAndStudentId(classId, studentId)) {
                throw new BusinessException("STUDENT_ALREADY_IN_CLASS",
                        "Học sinh này đã có trong lớp", HttpStatus.CONFLICT);
            }
            detail = userLookupPort.findUserDetails(List.of(studentId)).stream().findFirst().orElse(null);
        } else {
            studentId = accountPort.createStudentAccount(email, request.getFullName());
            memberRepository.save(new ClassMember(classId, studentId));
            detail = userLookupPort.findUserDetails(List.of(studentId)).stream().findFirst().orElse(null);
            return detail == null
                    ? new StudentResponse(studentId, request.getFullName(), email, null)
                    : new StudentResponse(detail.id(), detail.fullName(), detail.email(), null);
        }

        memberRepository.save(new ClassMember(classId, studentId));
        return detail == null
                ? new StudentResponse(studentId, "", email, null)
                : new StudentResponse(detail.id(), detail.fullName(), detail.email(), null);
    }

    /** AC-T00.4: rời lớp → không còn truy cập khóa học, nhưng kết quả học tập vẫn giữ. */
    @Transactional
    public void removeStudent(UUID teacherId, UUID classId, UUID studentId) {
        requireOwned(teacherId, classId);
        if (!memberRepository.existsByClassIdAndStudentId(classId, studentId)) {
            throw new BusinessException("STUDENT_NOT_IN_CLASS",
                    "Học sinh không thuộc lớp này", HttpStatus.NOT_FOUND);
        }
        memberRepository.deleteByClassIdAndStudentId(classId, studentId);
    }

    private SchoolClass requireOwned(UUID teacherId, UUID classId) {
        SchoolClass c = classRepository.findByIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new BusinessException("CLASS_NOT_FOUND",
                        "Không tìm thấy lớp học", HttpStatus.NOT_FOUND));
        if (!c.getOwnerTeacherId().equals(teacherId)) {
            throw new BusinessException("NOT_CLASS_OWNER",
                    "Bạn không phải giáo viên phụ trách lớp này", HttpStatus.FORBIDDEN);
        }
        return c;
    }
}
