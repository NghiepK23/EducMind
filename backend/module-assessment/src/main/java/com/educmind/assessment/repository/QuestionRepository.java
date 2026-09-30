package com.educmind.assessment.repository;

import com.educmind.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Optional<Question> findByIdAndDeletedAtIsNull(UUID id);

    /** TEACHER xem danh sách câu hỏi trong ngân hàng — E.6 GET /courses/{id}/questions. */
    List<Question> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID courseId);

    /** Lọc theo lesson cụ thể (dùng khi chọn câu hỏi thêm vào đề theo objective của 1 lesson). */
    List<Question> findByCourseIdAndLessonIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID courseId, UUID lessonId);
}
