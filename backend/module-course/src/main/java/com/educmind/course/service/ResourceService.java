package com.educmind.course.service;

import com.educmind.common.event.ResourceUploadedEvent;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.UserLookupPort;
import com.educmind.course.dto.ResourceResponse;
import com.educmind.course.entity.Course;
import com.educmind.course.entity.Lesson;
import com.educmind.course.entity.PublishStatus;
import com.educmind.course.entity.Resource;
import com.educmind.course.repository.LessonRepository;
import com.educmind.course.repository.ResourceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Logic nghiệp vụ Resource CRUD — Module03_Course_Lesson_Resource_FINAL.md, mục C.4-C.9.
 *
 * [QUYẾT ĐỊNH — Checkpoint #6] "Cách A": Resource kế thừa trạng thái publish của Lesson cha.
 * Nếu resource.lessonId != null và actor là STUDENT, phải check thêm lesson.publishStatus
 * (giống hệt LessonService), KHÔNG chỉ check enrollment. Resource cấp course (lessonId=null)
 * chỉ cần check enrollment, không có publishStatus nào để kiểm tra.
 *
 * [BỔ SUNG NGOÀI SPEC — Checkpoint #6] Spec gốc (mục C.6) chỉ liệt kê 5 endpoint, không có
 * endpoint liệt kê danh sách. Đã bổ sung listLessonResources/listCourseResources vì
 * ResourceRepository đã có sẵn 2 query method cho việc này và không có cách nào khác để
 * client biết resource nào tồn tại (phải biết trước UUID mới gọi GET /resources/{id} được).
 */
@Service
public class ResourceService {

    private static final long MAX_FILE_SIZE_BYTES = 52_428_800L; // 50MB — Module 3 mục C.5.6

    /**
     * Loại file mà module-rag đọc được (PDF, DOCX, TXT, MD). Chỉ các loại này đi qua pipeline RAG
     * (ở PROCESSING cho tới khi worker xong). Loại khác (ảnh, video, XLSX...) vẫn READY ngay như trước.
     */
    private static final Set<String> RAG_PROCESSABLE_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/markdown",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "video/mp4",
            "video/quicktime",
            "audio/mpeg"
    );

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final UserLookupPort userLookupPort;
    private final CourseAuthorizationHelper courseAuthorizationHelper;
    private final ResourceStorageService resourceStorageService;
    private final ApplicationEventPublisher eventPublisher;

    public ResourceService(ResourceRepository resourceRepository,
                            LessonRepository lessonRepository,
                            UserLookupPort userLookupPort,
                            CourseAuthorizationHelper courseAuthorizationHelper,
                            ResourceStorageService resourceStorageService,
                            ApplicationEventPublisher eventPublisher) {
        this.resourceRepository = resourceRepository;
        this.lessonRepository = lessonRepository;
        this.userLookupPort = userLookupPort;
        this.courseAuthorizationHelper = courseAuthorizationHelper;
        this.resourceStorageService = resourceStorageService;
        this.eventPublisher = eventPublisher;
    }

    /** POST /api/v1/lessons/{id}/resources — upload gắn vào 1 lesson cụ thể. */
    @Transactional
    public ResourceResponse createLessonResource(UUID actorId, UUID lessonId, MultipartFile file) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());
        courseAuthorizationHelper.assertCanManage(actor, course);

        return doCreateResource(actorId, course.getId(), lessonId, file);
    }

    /** POST /api/v1/courses/{id}/resources — upload cấp course, lessonId=NULL. */
    @Transactional
    public ResourceResponse createCourseResource(UUID actorId, UUID courseId, MultipartFile file) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertCanManage(actor, course);

        return doCreateResource(actorId, courseId, null, file);
    }

    /**
     * GET /api/v1/lessons/{id}/resources — [BỔ SUNG NGOÀI SPEC].
     * Áp dụng "Cách A": STUDENT phải enrolled + lesson phải PUBLISHED mới xem được danh sách.
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> listLessonResources(UUID actorId, UUID lessonId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Lesson lesson = loadLessonOrThrow(lessonId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(lesson.getCourseId());

        if ("STUDENT".equals(actor.role())) {
            assertStudentEnrolled(actorId, course);
            if (lesson.getPublishStatus() != PublishStatus.PUBLISHED) {
                throw lessonNotPublished();
            }
        } else {
            courseAuthorizationHelper.assertCanView(actor, course);
        }

        return resourceRepository.findByLessonIdAndDeletedAtIsNull(lessonId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * GET /api/v1/courses/{id}/resources — [BỔ SUNG NGOÀI SPEC].
     * Chỉ trả resource cấp course (lessonId=NULL) — không lẫn resource của từng lesson.
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> listCourseResources(UUID actorId, UUID courseId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);

        if ("STUDENT".equals(actor.role())) {
            assertStudentEnrolled(actorId, course);
        } else {
            courseAuthorizationHelper.assertCanView(actor, course);
        }

        return resourceRepository.findByCourseIdAndLessonIdIsNullAndDeletedAtIsNull(courseId)
                .stream().map(this::toResponse).toList();
    }

    /** GET /api/v1/resources/{id} — metadata. */
    @Transactional(readOnly = true)
    public ResourceResponse getResourceDetail(UUID actorId, UUID resourceId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Resource resource = loadResourceOrThrow(resourceId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(resource.getCourseId());

        assertViewAccess(actorId, actor, course, resource);
        return toResponse(resource);
    }

    /** GET /api/v1/resources/{id}/download — trả file thật (Phương án A, xem Checkpoint #6). */
    @Transactional(readOnly = true)
    public ResourceDownload downloadResource(UUID actorId, UUID resourceId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Resource resource = loadResourceOrThrow(resourceId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(resource.getCourseId());

        assertViewAccess(actorId, actor, course, resource);

        FileSystemResource file = resourceStorageService.load(resource.getStorageKey());
        return new ResourceDownload(file, resource.getFilename(), resource.getMimeType());
    }

    /** DELETE /api/v1/resources/{id} — soft-delete, chỉ owner/đồng giảng dạy course. */
    @Transactional
    public ResourceResponse deleteResource(UUID actorId, UUID resourceId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);
        Resource resource = loadResourceOrThrow(resourceId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(resource.getCourseId());
        courseAuthorizationHelper.assertCanManage(actor, course);

        resource.setDeletedAt(OffsetDateTime.now());
        resource = resourceRepository.saveAndFlush(resource);
        return toResponse(resource);
    }

    // ------------------------------------------------------------------
    // Helpers riêng
    // ------------------------------------------------------------------

    private ResourceResponse doCreateResource(UUID actorId, UUID courseId, UUID lessonId, MultipartFile file) {
        validateFile(file);

        ResourceStorageService.StoredFile stored;
        try {
            stored = resourceStorageService.store(courseId, file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được nội dung file upload", e);
        }

        Resource resource = new Resource(
                courseId,
                lessonId,
                file.getOriginalFilename(),
                file.getContentType(),
                stored.getSizeBytes(),
                stored.getStorageKey(),
                actorId
        );
        boolean ragProcessable = RAG_PROCESSABLE_MIME_TYPES.contains(file.getContentType());
        if (!ragProcessable) {
            resource.markReady(); // loại file RAG không đọc được: sẵn sàng ngay, chỉ không tìm kiếm được
        }

        resource = resourceRepository.save(resource);
        if (ragProcessable) {
            // Worker module-rag nhận sự kiện sau khi giao dịch commit rồi mới xử lý nền
            eventPublisher.publishEvent(new ResourceUploadedEvent(resource.getId()));
        }
        return toResponse(resource);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "VALIDATION_ERROR",
                    "File upload không được để trống",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(
                    "FILE_TOO_LARGE",
                    "File vượt quá giới hạn 50MB",
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException(
                    "UNSUPPORTED_FILE_TYPE",
                    "Định dạng file không được hỗ trợ",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Check quyền xem/tải 1 resource cụ thể — "Cách A" (Checkpoint #6):
     *   - TEACHER/SCHOOL_ADMIN: dùng assertCanView như thường lệ.
     *   - STUDENT: phải enrolled course; nếu resource.lessonId != null, lesson cha đó
     *     phải đang PUBLISHED (kế thừa trạng thái publish, resource không có cột riêng).
     */
    private void assertViewAccess(UUID actorId, UserLookupPort.UserSummary actor, Course course, Resource resource) {
        if ("STUDENT".equals(actor.role())) {
            assertStudentEnrolled(actorId, course);
            if (resource.getLessonId() != null) {
                Lesson lesson = loadLessonOrThrow(resource.getLessonId());
                if (lesson.getPublishStatus() != PublishStatus.PUBLISHED) {
                    throw lessonNotPublished();
                }
            }
        } else {
            courseAuthorizationHelper.assertCanView(actor, course);
        }
    }

    private void assertStudentEnrolled(UUID studentId, Course course) {
        if (!courseAuthorizationHelper.isStudentEnrolledInCourse(studentId, course)) {
            throw new BusinessException(
                    "CLASS_NOT_LINKED",
                    "Lớp của bạn chưa được gắn với khóa học này",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private BusinessException lessonNotPublished() {
        return new BusinessException(
                "LESSON_NOT_PUBLISHED",
                "Bài học này chưa được xuất bản",
                HttpStatus.FORBIDDEN
        );
    }

    private Resource loadResourceOrThrow(UUID resourceId) {
        return resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
                .orElseThrow(() -> new BusinessException(
                        "RESOURCE_NOT_FOUND",
                        "Không tìm thấy tài liệu",
                        HttpStatus.NOT_FOUND
                ));
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

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getCourseId(),
                resource.getLessonId(),
                resource.getFilename(),
                resource.getMimeType(),
                resource.getSizeBytes(),
                resource.getProcessingStatus(),
                resource.getUploadedBy(),
                resource.getCreatedAt(),
                resource.getUpdatedAt(),
                resource.getDeletedAt()
        );
    }

    /** Gói dữ liệu trả về cho endpoint download — file + metadata cần thiết để dựng HTTP header. */
    public static class ResourceDownload {
        private final FileSystemResource file;
        private final String filename;
        private final String mimeType;

        public ResourceDownload(FileSystemResource file, String filename, String mimeType) {
            this.file = file;
            this.filename = filename;
            this.mimeType = mimeType;
        }

        public FileSystemResource getFile() {
            return file;
        }

        public String getFilename() {
            return filename;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
