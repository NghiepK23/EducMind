package com.educmind.schoolclass.repository;

import com.educmind.schoolclass.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassMemberRepository extends JpaRepository<ClassMember, UUID> {

    List<ClassMember> findByClassId(UUID classId);

    List<ClassMember> findByUserId(UUID userId);

    Optional<ClassMember> findByClassIdAndUserId(UUID classId, UUID userId);

    boolean existsByClassIdAndUserId(UUID classId, UUID userId);

    /**
     * [MỚI] Khác existsByClassIdAndUserId ở trên: method cũ trả true kể cả khi user
     * đã rời lớp (left_at đã set). Method này chỉ true khi đang là thành viên ACTIVE
     * (left_at IS NULL) — dùng để kiểm tra trước khi cho DELETE /members/{userId}.
     */
    boolean existsByClassIdAndUserIdAndLeftAtIsNull(UUID classId, UUID userId);

    /** [MỚI] Danh sách thành viên đang active của 1 lớp. */
    List<ClassMember> findByClassIdAndLeftAtIsNull(UUID classId);
}
