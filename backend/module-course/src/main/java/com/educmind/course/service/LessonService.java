package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.UserLookupPort;
import com.educmind.course.dto.CreateLessonRequest;
import com.educmind.course.dto.LearningObjectiveInput;
import com.educmind.course.dto.LessonResponse;
import com.educmind.course.dto.UpdateLessonRequest;
import com.educmind.course.entity.Course;
import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.Lesson;
import com.educmind.course.entity.PublishStatus;
import com.educmind.course.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Logic nghiệp vụ Lesson CRUD + publish/unpublish —
 * Module03_Course_Lesson_Resource_FINAL.md, mục C.4/C.5/C.6/C.9.
 *
 * [QUAN TRỌNG - khác biệt so với CourseService] Việc xem Lesson của STUDENT KHÔNG dùng
 * courseAuthorizationHelper.assertCanView (method đó chỉ xử lý SCHOOL_ADMIN + TEACHER,
 * gọi assertCanManage cho mọi role khác — sẽ luôn forbidden với STUDENT vì học sinh
 * không phải created_by cũng không đồng giảng dạy). STUDENT có luồng riêng:
 *   1. Phải isStudentEnrolledInCourse (lớp mình đã link course) — nếu không: CLASS_NOT_LINKED.
 *   2. Lesson phải publishStatus = PUBLISHED — nếu không (đang DRAFT): LESSON_NOT_PUBLISHED.
 * 2 mã lỗi này đã có sẵn ở Module 3 mục C.8 (dùng chung cho Course lẫn Lesson).
 */
@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserLookupPort userLookupPort;
    private final CourseAuthorizationHelper courseAuthorizationHelper;

    public LessonService(LessonRepository lessonRepository,
                          UserLookupPort userLookupPort,
                          CourseAuthorizationHelper courseAuthorizationHelper) {
        this.lessonRepository = lessonRepository;
        this.userLookupPort = userLookupPort;
        this.courseAuthorizationHelper = courseAuthorizationHelper;
    }

    /** POST /api/v1/courses/{id}/lessons — chỉ owner/đồng giảng dạy course. */
    @Transactional
    public LessonResponse createLesson(UUID actorId, UUID courseId, CreateLessonRequest request) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertCanManage(actor, course);

        Lesson lesson = new Lesson(courseId, request.getTitle(), actorId);
        lesson.setDescription(request.getDescription());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setLearningObjectives(mapToEntityObjectives(request.getLearningObjectives(), null));

        lesson = lessonRepository.save(lesson);
        return toResponse(lesson);
    }

    /**
     * GET /api/v1/courses/{id}/lessons — TEACHER/SCHOOL_ADMIN thấy mọi publishStatus,
     * STUDENT chỉ thấy PUBLISHED (và phải thuộc lớp đã link course, nếu không thì
     * CLASS_NOT_LINKED — không âm thầm trả danh sách rỗng, vì đó là 2 tình huống khác nhau
     * về mặt UX: "lớp bạn chưa học course này" khác với "course này chưa có lesson nào").
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> listLessons(UUID actorId, UUID courseId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);

        List<Lesson> lessons;
        if ("STUDENT".equals(actor.role())) {
            assertStudentEnrolled(actorId, course);
            lessons = lessonRepository.findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
                    courseId, PublishStatus.PUBLISHED);
        } else {
            courseAuthorizationHelper.assertCanView(actor, course);
            lessons = lessonRepository.findByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(courseId);
        }

        return lessons.stream().map(this::toResponse).toList();
    }

    /** GET /api/v1/lessons/{id} — xem chi tiết 1 lesson. */
    @Transactional(readOnly = true)
    public LessonResponse getLessonDetail(UUID actorId, UUID lessonId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());

        if ("STUDENT".equals(actor.role())) {
            assertStudentEnrolled(actorId, course);
            if (lesson.getPublishStatus() != PublishStatus.PUBLISHED) {
                throw new BusinessException(
                        "LESSON_NOT_PUBLISHED",
                        "Bài học này chưa được xuất bản",
                        HttpStatus.FORBIDDEN
                );
            }
        } else {
            courseAuthorizationHelper.assertCanView(actor, course);
        }

        return toResponse(lesson);
    }

    /** PATCH /api/v1/lessons/{id} — chỉ owner/đồng giảng dạy course chứa lesson. */
    @Transactional
    public LessonResponse updateLesson(UUID actorId, UUID lessonId, UpdateLessonRequest request) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());
        courseAuthorizationHelper.assertCanManage(actor, course);

        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new BusinessException(
                        "VALIDATION_ERROR",
                        "title không được để trống",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            lesson.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            lesson.setDescription(request.getDescription());
        }
        if (request.getLearningObjectives() != null) {
            lesson.setLearningObjectives(
                    mapToEntityObjectives(request.getLearningObjectives(), lesson.getLearningObjectives()));
        }
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }

        lesson = lessonRepository.saveAndFlush(lesson);
        return toResponse(lesson);
    }

    /** DELETE /api/v1/lessons/{id} — soft-delete, chỉ owner/đồng giảng dạy course. */
    @Transactional
    public LessonResponse deleteLesson(UUID actorId, UUID lessonId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());
        courseAuthorizationHelper.assertCanManage(actor, course);

        lesson.setDeletedAt(OffsetDateTime.now());
        lesson = lessonRepository.saveAndFlush(lesson);
        return toResponse(lesson);
    }

    /** PATCH /api/v1/lessons/{id}/publish — chỉ owner/đồng giảng dạy course. */
    @Transactional
    public LessonResponse publishLesson(UUID actorId, UUID lessonId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());
        courseAuthorizationHelper.assertCanManage(actor, course);

        lesson.publish();
        lesson = lessonRepository.saveAndFlush(lesson);
        return toResponse(lesson);
    }

    /** PATCH /api/v1/lessons/{id}/unpublish — chỉ owner/đồng giảng dạy course. */
    @Transactional
    public LessonResponse unpublishLesson(UUID actorId, UUID lessonId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());
        courseAuthorizationHelper.assertCanManage(actor, course);

        lesson.unpublish();
        lesson = lessonRepository.saveAndFlush(lesson);
        return toResponse(lesson);
    }

    // ------------------------------------------------------------------
    // Helpers riêng
    // ------------------------------------------------------------------

    private void assertStudentEnrolled(UUID studentId, Course course) {
        if (!courseAuthorizationHelper.isStudentEnrolledInCourse(studentId, course)) {
            throw new BusinessException(
                    "CLASS_NOT_LINKED",
                    "Lớp của bạn chưa được gắn với khóa học này",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    /**
     * Map input (chưa có id) sang entity LearningObjective (có id).
     * [ĐÃ SỬA] Nếu 1 input có text trùng với 1 objective trong danh sách "existing" (bản cũ
     * trước khi update), giữ nguyên id cũ thay vì sinh random mới — tránh phá vỡ liên kết
     * nếu tương lai có gì tham chiếu tới learningObjective.id. Chỉ objective có text hoàn
     * toàn mới (chưa từng xuất hiện ở bản cũ) mới được sinh id mới.
     * existing = null hoặc rỗng (trường hợp createLesson, chưa có gì cũ) → mọi input đều
     * là mới, sinh id mới toàn bộ như hành vi ban đầu.
     */
    private List<LearningObjective> mapToEntityObjectives(List<LearningObjectiveInput> inputs,
                                                            List<LearningObjective> existing) {
        List<LearningObjective> result = new ArrayList<>();
        if (inputs == null) {
            return result;
        }
        Map<String, UUID> existingIdByText = new HashMap<>();
        if (existing != null) {
            for (LearningObjective old : existing) {
                existingIdByText.putIfAbsent(old.getText(), old.getId());
            }
        }
        for (LearningObjectiveInput input : inputs) {
            UUID id = existingIdByText.remove(input.getText());
            if (id == null) {
                id = UUID.randomUUID();
            }
            result.add(new LearningObjective(id, input.getText(), input.getBloomLevel()));
        }
        return result;
    }

    private Lesson loadLessonOrThrow(UUID lessonId) {
        return lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .orElseThrow(() -> new BusinessException(
                        "LESSON_NOT_FOUND",
                        "Không tìm thấy bài học",
                        HttpStatus.NOT_FOUND
                ));
    }

    private UserLookupPort.UserSummary loadActor(UUID actorId) {
        return userLookupPort.findUserSummary(actorId)
                .orElseThrow(() -> new BusinessException(
                        "ACTOR_NOT_FOUND",
                        "Không tìm thấy thông tin tài khoản của bạn",
                        HttpStatus.UNAUTHORIZED
                ));
    }

    private LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getCourseId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getLearningObjectives(),
                lesson.getOrderIndex(),
                lesson.getPublishStatus(),
                lesson.getPublishedAt(),
                lesson.getCreatedBy(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt(),
                lesson.getDeletedAt()
        );
    }
}
