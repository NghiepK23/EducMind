package com.educmind.assessment.repository;

import com.educmind.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Optional<Assessment> findByIdAndDeletedAtIsNull(UUID id);

    /** Danh sách đề kiểm tra trong 1 course (dùng cho màn hình quản lý đề của giáo viên). */
    List<Assessment> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID courseId);
}
