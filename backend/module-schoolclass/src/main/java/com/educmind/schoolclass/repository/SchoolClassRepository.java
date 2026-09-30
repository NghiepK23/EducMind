package com.educmind.schoolclass.repository;

import com.educmind.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GET /api/v1/classes — Module01_Auth_SchoolClass_FINAL.md, mục B.6/B.7.
 * Mọi phương thức list/get ở đây đều loại trừ lớp đã soft-delete (deletedAt IS NULL),
 * vì bảng classes không có Hibernate filter tự động ({@code ddl-auto=validate},
 * không dùng @SQLRestriction) — phải tự lọc thủ công ở tầng query.
 */
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findBySchoolId(UUID schoolId);

    List<SchoolClass> findByOwnerTeacherId(UUID ownerTeacherId);

    /** SCHOOL_ADMIN: mọi lớp còn hoạt động trong trường mình. */
    List<SchoolClass> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);

    /** SCHOOL_ADMIN + lọc hẹp theo 1 giáo viên cụ thể (query param teacherId). */
    List<SchoolClass> findBySchoolIdAndOwnerTeacherIdAndDeletedAtIsNull(UUID schoolId, UUID ownerTeacherId);

    /**
     * [MỚI] Dùng cho GET/{id}, PATCH/{id}, DELETE/{id} — luôn lọc deletedAt IS NULL,
     * nếu không sẽ vô tình cho phép xem/sửa lại 1 lớp đã bị soft-delete trước đó.
     */
    Optional<SchoolClass> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * TEACHER: lớp mình là owner_teacher_id HOẶC có mặt trong class_members
     * (đồng giảng dạy — quyền xem, theo B.7) và chưa rời lớp (left_at IS NULL).
     */
    @Query("SELECT DISTINCT c FROM SchoolClass c " +
            "WHERE c.deletedAt IS NULL AND (" +
            "  c.ownerTeacherId = :teacherId " +
            "  OR c.id IN (SELECT cm.classId FROM ClassMember cm WHERE cm.userId = :teacherId AND cm.leftAt IS NULL)" +
            ")")
    List<SchoolClass> findAccessibleByTeacher(@Param("teacherId") UUID teacherId);
}
