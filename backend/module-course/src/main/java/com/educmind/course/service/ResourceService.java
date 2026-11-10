package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.course.dto.ResourceResponse;
import com.educmind.course.entity.ContentStatus;
import com.educmind.course.entity.Course;
import com.educmind.course.entity.CourseTopic;
import com.educmind.course.entity.Lesson;
import com.educmind.course.entity.Resource;
import com.educmind.course.repository.CourseTopicRepository;
import com.educmind.course.repository.LessonRepository;
import com.educmind.course.repository.ResourceRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tài liệu học tập — US-T02 (AC-T02.3: ≤ 50MB, đúng định dạng), US-S01 (AC-S01.3: học sinh
 * xem/tải tài liệu của bài học và tài liệu chung của khóa học).
 */
@Service
public class ResourceService {

    public static final long MAX_FILE_SIZE = 52_428_800L; // 50 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md",
            "png", "jpg", "jpeg", "gif", "mp4", "mp3", "zip"
    );

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final CourseTopicRepository topicRepository;
    private final ResourceStorageService storage;
    private final CoursePermission permission;

    public ResourceService(ResourceRepository resourceRepository,
                           LessonRepository lessonRepository,
                           CourseTopicRepository topicRepository,
                           ResourceStorageService storage,
                           CoursePermission permission) {
        this.resourceRepository = resourceRepository;
        this.lessonRepository = lessonRepository;
        this.topicRepository = topicRepository;
        this.storage = storage;
        this.permission = permission;
    }

    public record Download(String fileName, String contentType, FileSystemResource file) {
    }

    @Transactional
    public ResourceResponse uploadToLesson(UUID teacherId, UUID lessonId, MultipartFile file) {
        Lesson lesson = permission.requireManagedLesson(teacherId, lessonId);
        validate(file);

        String key = storage.store(lesson.getCourseId(), file);
        Resource resource = new Resource(lesson.getCourseId(), lessonId, safeName(file),
                key, file.getSize(), contentType(file), teacherId);
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse uploadToCourse(UUID teacherId, UUID courseId, MultipartFile file) {
        Course course = permission.requireManagedCourse(teacherId, courseId);
        validate(file);

        String key = storage.store(course.getId(), file);
        Resource resource = new Resource(courseId, null, safeName(file),
                key, file.getSize(), contentType(file), teacherId);
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listForLesson(UUID actorId, String role, UUID lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedAtIsNull(lessonId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài học"));
        permission.requireVisibleCourse(actorId, role, lesson.getCourseId());

        if (!CoursePermission.isTeacher(role) && !lessonVisibleForStudent(lesson)) {
            throw new BusinessException("LESSON_NOT_PUBLISHED",
                    "Bài học chưa được xuất bản", HttpStatus.FORBIDDEN);
        }

        List<ResourceResponse> result = new ArrayList<>();
        for (Resource r : resourceRepository.findByLessonIdAndDeletedAtIsNullOrderByUploadedAtDesc(lessonId)) {
            result.add(ResourceResponse.from(r));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listForCourse(UUID actorId, String role, UUID courseId) {
        permission.requireVisibleCourse(actorId, role, courseId);

        List<ResourceResponse> result = new ArrayList<>();
        for (Resource r : resourceRepository.findByCourseIdAndLessonIdIsNullAndDeletedAtIsNullOrderByUploadedAtDesc(courseId)) {
            result.add(ResourceResponse.from(r));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Download download(UUID actorId, String role, UUID resourceId) {
        Resource resource = resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy tài liệu"));

        if (resource.getLessonId() != null) {
            Lesson lesson = lessonRepository.findByIdAndDeletedAtIsNull(resource.getLessonId())
                    .orElseThrow(() -> permission.notFound("Không tìm thấy bài học"));
            permission.requireVisibleCourse(actorId, role, lesson.getCourseId());
            if (!CoursePermission.isTeacher(role) && !lessonVisibleForStudent(lesson)) {
                throw new BusinessException("LESSON_NOT_PUBLISHED",
                        "Bài học chưa được xuất bản", HttpStatus.FORBIDDEN);
            }
        } else {
            permission.requireVisibleCourse(actorId, role, resource.getCourseId());
        }

        return new Download(resource.getFileName(), resource.getFileType(),
                storage.load(resource.getFileUrl()));
    }

    /** Xóa mềm bản ghi (kết quả học tập không liên quan), xóa luôn tệp trên đĩa. */
    @Transactional
    public void delete(UUID teacherId, UUID resourceId) {
        Resource resource = resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy tài liệu"));

        if (resource.getLessonId() != null) {
            permission.requireManagedLesson(teacherId, resource.getLessonId());
        } else {
            permission.requireManagedCourse(teacherId, resource.getCourseId());
        }

        resource.markDeleted();
        resourceRepository.save(resource);
        storage.delete(resource.getFileUrl());
    }

    private boolean lessonVisibleForStudent(Lesson lesson) {
        boolean topicPublished = topicRepository.findByIdAndDeletedAtIsNull(lesson.getTopicId())
                .map(CourseTopic::getStatus)
                .map(s -> s == ContentStatus.PUBLISHED)
                .orElse(false);
        return topicPublished && lesson.getStatus() == ContentStatus.PUBLISHED;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Tệp rỗng", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE",
                    "Tệp vượt quá 50MB", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String extension = ResourceStorageService.extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE",
                    "Định dạng tệp không được hỗ trợ: ." + extension, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private String safeName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "tai-lieu";
        }
        return name.replaceAll("[\\\\/]", "_");
    }

    private String contentType(MultipartFile file) {
        String type = file.getContentType();
        if (type == null || type.isBlank()) {
            String ext = ResourceStorageService.extensionOf(file.getOriginalFilename());
            return ext.isEmpty() ? "application/octet-stream" : "application/" + ext;
        }
        return type.length() > 50 ? type.substring(0, 50) : type;
    }
}
