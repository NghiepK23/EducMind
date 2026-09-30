package com.educmind.course.repository;

import com.educmind.course.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD cơ bản cho `subjects` — entity MỚI, phát sinh từ thay đổi kiến trúc
 * (xem checkpoint "THAY ĐỔI KIẾN TRÚC LỚN"). Môn học do SCHOOL_ADMIN tạo sẵn
 * cho trường mình, giáo viên chọn 1 subject khi tạo Course.
 *
 * Cùng lý do như CourseRepository: ddl-auto=validate, không dùng @SQLRestriction,
 * nên phải tự lọc deletedAt IS NULL thủ công ở tầng query.
 */
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    /** Danh sách môn học còn hoạt động của 1 trường — dùng khi giáo viên chọn subject lúc tạo course. */
    List<Subject> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);

    /**
     * Dùng cho GET/{id}, PATCH/{id}, DELETE/{id} và để validate subjectId có tồn tại/hợp lệ
     * khi tạo course — luôn lọc deletedAt IS NULL, tránh dùng lại 1 subject đã bị xóa mềm.
     */
    Optional<Subject> findByIdAndDeletedAtIsNull(UUID id);
}
