package com.educmind.analytics.service;

import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.entity.Mastery;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/v1/students/me/mastery (Module05 FINAL muc 5.1).
 * Hoc sinh chi xem duoc mastery cua chinh minh (studentId lay tu token, khong nhan tu client).
 * Bat buoc dung 1 trong 2 bo loc: lesson_id hoac course_id.
 */
@Service
public class StudentMasteryQueryService {

    private final MasteryRepository masteryRepository;

    public StudentMasteryQueryService(MasteryRepository masteryRepository) {
        this.masteryRepository = masteryRepository;
    }

    @Transactional(readOnly = true)
    public List<MasteryResponse> findMine(UUID studentId, UUID lessonId, UUID courseId) {
        if ((lessonId == null) == (courseId == null)) {
            throw new BusinessException("INVALID_FILTER",
                    "Cần truyền đúng 1 trong 2 tham số: lesson_id hoặc course_id",
                    HttpStatus.BAD_REQUEST);
        }
        List<Mastery> rows = lessonId != null
                ? masteryRepository.findByStudentIdAndLessonId(studentId, lessonId)
                : masteryRepository.findByStudentIdAndCourseId(studentId, courseId);
        return rows.stream().map(MasteryResponse::new).toList();
    }
}
