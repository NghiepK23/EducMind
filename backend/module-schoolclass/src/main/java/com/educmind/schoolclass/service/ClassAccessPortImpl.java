package com.educmind.schoolclass.service;

import com.educmind.common.port.ClassAccessPort;
import com.educmind.schoolclass.entity.ClassMember;
import com.educmind.schoolclass.entity.MemberRole;
import com.educmind.schoolclass.entity.SchoolClass;
import com.educmind.schoolclass.repository.ClassMemberRepository;
import com.educmind.schoolclass.repository.SchoolClassRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation thật của ClassAccessPort (định nghĩa ở educmind-common).
 * Đây là "adapter" trong kiến trúc Ports-and-Adapters — nơi duy nhất trong
 * toàn hệ thống được phép JOIN dữ liệu SchoolClass + ClassMember trực tiếp,
 * vì cả 2 entity đều thuộc module này.
 *
 * Spring sẽ tự động đăng ký bean này và "nối dây" cho bất kỳ module nào
 * (vd module-auth, module-course) khai báo @Autowired/constructor-inject
 * ClassAccessPort.
 */
@Component
public class ClassAccessPortImpl implements ClassAccessPort {

    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository classMemberRepository;

    public ClassAccessPortImpl(
            SchoolClassRepository schoolClassRepository,
            ClassMemberRepository classMemberRepository
    ) {
        this.schoolClassRepository = schoolClassRepository;
        this.classMemberRepository = classMemberRepository;
    }

    @Override
    public boolean isOwnerOrCoTeacher(UUID teacherId, UUID classId) {
        Optional<SchoolClass> schoolClass = schoolClassRepository.findById(classId);
        if (schoolClass.isEmpty()) {
            return false;
        }

        if (schoolClass.get().getOwnerTeacherId().equals(teacherId)) {
            return true;
        }

        return classMemberRepository.findByClassIdAndUserId(classId, teacherId)
                .filter(member -> member.getMemberRole() == MemberRole.TEACHER)
                .filter(member -> member.getLeftAt() == null)
                .isPresent();
    }

    @Override
    public boolean isActiveMember(UUID userId, UUID classId) {
        Optional<SchoolClass> schoolClass = schoolClassRepository.findById(classId);
        if (schoolClass.isEmpty()) {
            return false;
        }

        if (schoolClass.get().getOwnerTeacherId().equals(userId)) {
            return true;
        }

        return classMemberRepository.findByClassIdAndUserId(classId, userId)
                .filter(member -> member.getLeftAt() == null)
                .isPresent();
    }

    @Override
    public void addMember(UUID classId, UUID userId, boolean isTeacherRole) {
        MemberRole memberRole = isTeacherRole ? MemberRole.TEACHER : MemberRole.STUDENT;
        ClassMember classMember = new ClassMember(classId, userId, memberRole);
        classMemberRepository.save(classMember);
    }

    @Override
    public Optional<UUID> findSchoolIdByClassId(UUID classId) {
        return schoolClassRepository.findById(classId)
                .map(SchoolClass::getSchoolId);
    }

    @Override
    public java.util.List<UUID> findAccessibleClassIdsForTeacher(UUID teacherId) {
        return schoolClassRepository.findAccessibleByTeacher(teacherId).stream()
                .map(SchoolClass::getId)
                .toList();
    }

}
