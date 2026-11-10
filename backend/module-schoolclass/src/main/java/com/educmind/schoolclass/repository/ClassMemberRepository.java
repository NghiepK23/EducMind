package com.educmind.schoolclass.repository;

import com.educmind.schoolclass.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassMemberRepository extends JpaRepository<ClassMember, UUID> {

    boolean existsByClassIdAndStudentId(UUID classId, UUID studentId);

    Optional<ClassMember> findByClassIdAndStudentId(UUID classId, UUID studentId);

    List<ClassMember> findByClassIdOrderByJoinedAtAsc(UUID classId);

    List<ClassMember> findByStudentId(UUID studentId);

    @Query("select m.studentId from ClassMember m where m.classId = :classId")
    List<UUID> findStudentIdsByClassId(@Param("classId") UUID classId);

    void deleteByClassIdAndStudentId(UUID classId, UUID studentId);
}
