package com.educmind.schoolclass.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.schoolclass.entity.ClassMember;
import com.educmind.schoolclass.entity.SchoolClass;
import com.educmind.schoolclass.repository.ClassMemberRepository;
import com.educmind.schoolclass.repository.SchoolClassRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClassAccessPortImpl implements ClassAccessPort {

    private final SchoolClassRepository classRepository;
    private final ClassMemberRepository memberRepository;

    public ClassAccessPortImpl(SchoolClassRepository classRepository,
                               ClassMemberRepository memberRepository) {
        this.classRepository = classRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean isOwner(UUID teacherId, UUID classId) {
        return classRepository.findByIdAndDeletedAtIsNull(classId)
                .map(c -> c.getOwnerTeacherId().equals(teacherId))
                .orElse(false);
    }

    @Override
    public boolean isMember(UUID studentId, UUID classId) {
        return classRepository.existsByIdAndDeletedAtIsNull(classId)
                && memberRepository.existsByClassIdAndStudentId(classId, studentId);
    }

    @Override
    @Transactional
    public void addStudent(UUID classId, UUID studentId) {
        if (!classRepository.existsByIdAndDeletedAtIsNull(classId)) {
            throw new BusinessException("CLASS_NOT_FOUND", "Không tìm thấy lớp học", HttpStatus.NOT_FOUND);
        }
        if (!memberRepository.existsByClassIdAndStudentId(classId, studentId)) {
            memberRepository.save(new ClassMember(classId, studentId));
        }
    }

    @Override
    public List<UUID> findStudentIds(UUID classId) {
        return memberRepository.findStudentIdsByClassId(classId);
    }

    @Override
    public List<UUID> findClassIdsForTeacher(UUID teacherId) {
        return classRepository.findByOwnerTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(teacherId)
                .stream().map(SchoolClass::getId).toList();
    }

    @Override
    public List<UUID> findClassIdsForStudent(UUID studentId) {
        return memberRepository.findByStudentId(studentId).stream()
                .filter(m -> classRepository.existsByIdAndDeletedAtIsNull(m.getClassId()))
                .map(ClassMember::getClassId)
                .distinct()
                .toList();
    }

    @Override
    public Optional<UUID> findOwnerTeacherId(UUID classId) {
        return classRepository.findByIdAndDeletedAtIsNull(classId).map(SchoolClass::getOwnerTeacherId);
    }
}
