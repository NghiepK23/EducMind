package com.educmind.analytics.service;

import com.educmind.analytics.dto.MasteryMatrixResponse;
import com.educmind.analytics.entity.Mastery;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LessonAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GET /api/v1/lessons/{lessonId}/analytics/mastery-matrix?class_id=
 * (Module05 FINAL muc 5.2, KHONG phan trang).
 *
 * Thu tu kiem tra: class_id null (400) -> quyen owner/dong giang day cua course chua lesson (403)
 * -> truy van. Lesson khong ton tai cung ra 403.
 * Cot = objectives cua lesson (ke ca objective chua co ai lam bai).
 * Hang = hoc sinh ACTIVE cua lop (course_members.source_class_id); hoc sinh chua co mastery van co hang,
 * o chua co du lieu la null. Lop khong co hoc sinh -> students rong, 200.
 */
@Service
public class TeacherMasteryMatrixQueryService {

    private final MasteryRepository masteryRepository;
    private final CourseAccessPort courseAccessPort;
    private final LessonAccessPort lessonAccessPort;

    public TeacherMasteryMatrixQueryService(MasteryRepository masteryRepository,
                                            CourseAccessPort courseAccessPort,
                                            LessonAccessPort lessonAccessPort) {
        this.masteryRepository = masteryRepository;
        this.courseAccessPort = courseAccessPort;
        this.lessonAccessPort = lessonAccessPort;
    }

    @Transactional(readOnly = true)
    public MasteryMatrixResponse find(UUID actorId, UUID lessonId, UUID classId) {
        if (classId == null) {
            throw new BusinessException("INVALID_FILTER",
                    "class_id là bắt buộc", HttpStatus.BAD_REQUEST);
        }
        UUID courseId = lessonAccessPort.findCourseIdByLessonId(lessonId).orElse(null);
        if (courseId == null || !courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("FORBIDDEN_NOT_COURSE_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem mastery matrix",
                    HttpStatus.FORBIDDEN);
        }

        List<MasteryMatrixResponse.ObjectiveColumn> columns = lessonAccessPort
                .findObjectivesByLessonId(lessonId).stream()
                .map(o -> new MasteryMatrixResponse.ObjectiveColumn(o.id(), o.text()))
                .toList();

        List<UUID> studentIds = courseAccessPort
                .findActiveStudentIdsByCourseAndSourceClass(courseId, classId);
        if (studentIds.isEmpty()) {
            return new MasteryMatrixResponse(lessonId, classId, columns, List.of());
        }

        Map<UUID, Map<UUID, Mastery>> byStudent = new HashMap<>();
        for (Mastery m : masteryRepository.findByLessonIdAndStudentIdIn(lessonId, studentIds)) {
            byStudent.computeIfAbsent(m.getStudentId(), k -> new HashMap<>())
                    .put(m.getLearningObjectiveId(), m);
        }

        List<MasteryMatrixResponse.StudentRow> rows = new ArrayList<>();
        for (UUID studentId : studentIds) {
            Map<UUID, Mastery> own = byStudent.getOrDefault(studentId, Map.of());
            List<MasteryMatrixResponse.Cell> cells = new ArrayList<>();
            for (MasteryMatrixResponse.ObjectiveColumn col : columns) {
                Mastery m = own.get(col.getId());
                cells.add(m == null ? null : new MasteryMatrixResponse.Cell(m));
            }
            rows.add(new MasteryMatrixResponse.StudentRow(studentId, cells));
        }
        return new MasteryMatrixResponse(lessonId, classId, columns, rows);
    }
}
