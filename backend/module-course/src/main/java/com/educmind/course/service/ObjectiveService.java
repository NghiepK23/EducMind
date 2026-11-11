package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.LearningEvidencePort;
import com.educmind.course.dto.ObjectiveRequest;
import com.educmind.course.dto.ObjectiveResponse;
import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.LearningObjectiveRepository;
import com.educmind.course.repository.LessonRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mục tiêu học tập (Spec v2: bảng riêng, UNIQUE(lesson_id, code)).
 * AC-T02.2: hai mục tiêu trong cùng bài học không được trùng mã.
 * AC-T11.1: đổi mã mục tiêu ĐÃ CÓ kết quả đánh giá bị chặn (409 OBJECTIVE_IN_USE);
 * mục tiêu chưa có bằng chứng thì vẫn đổi mã được (và luôn sửa được mô tả).
 */
@Service
public class ObjectiveService {

    private final LearningObjectiveRepository objectiveRepository;
    private final LessonRepository lessonRepository;
    private final CoursePermission permission;
    private final LearningEvidencePort evidencePort;

    public ObjectiveService(LearningObjectiveRepository objectiveRepository,
                            LessonRepository lessonRepository,
                            CoursePermission permission,
                            LearningEvidencePort evidencePort) {
        this.objectiveRepository = objectiveRepository;
        this.lessonRepository = lessonRepository;
        this.permission = permission;
        this.evidencePort = evidencePort;
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

        String newCode = request.getCode() == null ? null : request.getCode().trim();
        if (newCode != null && !newCode.isEmpty() && !newCode.equalsIgnoreCase(objective.getCode())) {
            // AC-T11.1: mục tiêu đã có kết quả đánh giá thì không được đổi mã.
            if (evidencePort.hasEvidenceForObjective(objectiveId)) {
                throw new BusinessException("OBJECTIVE_IN_USE",
                        "Mục tiêu đã có kết quả đánh giá, không thể đổi mã", HttpStatus.CONFLICT);
            }
            if (objectiveRepository.existsByLessonIdAndCodeIgnoreCase(objective.getLessonId(), newCode)) {
                throw new BusinessException("DUPLICATE_OBJECTIVE_CODE",
                        "Mã mục tiêu đã tồn tại trong bài học này", HttpStatus.CONFLICT);
            }
            objective.updateCode(newCode);
        }
        objective.updateDescription(request.getDescription().trim());
        return ObjectiveResponse.from(objectiveRepository.save(objective));
    }

    @Transactional
    public void delete(UUID teacherId, UUID objectiveId) {
        LearningObjective objective = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy mục tiêu học tập"));
        permission.requireManagedLesson(teacherId, objective.getLessonId());

        // Mục tiêu đã được bài tập/câu hỏi tham chiếu thì không xoá cứng được (FK).
        objectiveRepository.delete(objective);
        try {
            objectiveRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("OBJECTIVE_IN_USE",
                    "Mục tiêu đã được dùng cho bài tập hoặc câu hỏi, không thể xoá",
                    HttpStatus.CONFLICT);
        }
    }
}
