package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.course.dto.ObjectiveRequest;
import com.educmind.course.dto.ObjectiveResponse;
import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.LearningObjectiveRepository;
import com.educmind.course.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mục tiêu học tập (Spec v2: bảng riêng, UNIQUE(lesson_id, code)).
 * AC-T02.2: hai mục tiêu trong cùng bài học không được trùng mã.
 * AC-T11.1: đổi mã mục tiêu đã có kết quả đánh giá bị chặn — ở phase 1 dữ liệu đánh giá
 * chưa có module, nên mã được giữ bất biến sau khi tạo (an toàn hơn, xem MIGRATION-STATUS).
 */
@Service
public class ObjectiveService {

    private final LearningObjectiveRepository objectiveRepository;
    private final LessonRepository lessonRepository;
    private final CoursePermission permission;

    public ObjectiveService(LearningObjectiveRepository objectiveRepository,
                            LessonRepository lessonRepository,
                            CoursePermission permission) {
        this.objectiveRepository = objectiveRepository;
        this.lessonRepository = lessonRepository;
        this.permission = permission;
    }

    @Transactional(readOnly = true)
    public List<ObjectiveResponse> list(UUID actorId, String role, UUID lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài học"));
        permission.requireVisibleCourse(actorId, role, lesson.getCourseId());

        List<ObjectiveResponse> result = new ArrayList<>();
        for (LearningObjective o : objectiveRepository.findByLessonIdOrderByCodeAsc(lessonId)) {
            result.add(ObjectiveResponse.from(o));
        }
        return result;
    }

    @Transactional
    public ObjectiveResponse create(UUID teacherId, UUID lessonId, ObjectiveRequest request) {
        permission.requireManagedLesson(teacherId, lessonId);

        String code = request.getCode().trim();
        if (objectiveRepository.existsByLessonIdAndCodeIgnoreCase(lessonId, code)) {
            throw new BusinessException("DUPLICATE_OBJECTIVE_CODE",
                    "Mã mục tiêu đã tồn tại trong bài học này", HttpStatus.CONFLICT);
        }

        LearningObjective objective = new LearningObjective(lessonId, code, request.getDescription().trim());
        return ObjectiveResponse.from(objectiveRepository.save(objective));
    }

    @Transactional
    public ObjectiveResponse update(UUID teacherId, UUID objectiveId, ObjectiveRequest request) {
        LearningObjective objective = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy mục tiêu học tập"));
        permission.requireManagedLesson(teacherId, objective.getLessonId());

        // Mã giữ nguyên (AC-T11.1); chỉ cập nhật mô tả.
        objective.updateDescription(request.getDescription().trim());
        return ObjectiveResponse.from(objectiveRepository.save(objective));
    }

    @Transactional
    public void delete(UUID teacherId, UUID objectiveId) {
        LearningObjective objective = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy mục tiêu học tập"));
        permission.requireManagedLesson(teacherId, objective.getLessonId());
        objectiveRepository.delete(objective);
    }
}
