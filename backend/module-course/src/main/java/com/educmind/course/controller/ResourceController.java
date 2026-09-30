package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.ResourceResponse;
import com.educmind.course.service.ResourceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Module03_Course_Lesson_Resource_FINAL.md, mục C.6, cộng 2 endpoint list
 * [BỔ SUNG NGOÀI SPEC — Checkpoint #6, xem ResourceService].
 *
 * Không dùng @RequestMapping ở class, cùng lý do như LessonController — endpoint trải
 * trên nhiều base path khác nhau (/lessons/{id}/resources, /courses/{id}/resources,
 * /resources/{id}).
 *
 * downloadResource KHÔNG bọc trong ApiResponse — phải trả file nhị phân thật, không phải JSON,
 * nên dùng ResponseEntity<FileSystemResource> trực tiếp kèm header Content-Disposition.
 */
@RestController
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(value = "/api/v1/lessons/{lessonId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceResponse>> createLessonResource(
            Authentication authentication,
            @PathVariable UUID lessonId,
            @RequestParam("file") MultipartFile file
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ResourceResponse response = resourceService.createLessonResource(actorId, lessonId, file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(value = "/api/v1/courses/{courseId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceResponse>> createCourseResource(
            Authentication authentication,
            @PathVariable UUID courseId,
            @RequestParam("file") MultipartFile file
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ResourceResponse response = resourceService.createCourseResource(actorId, courseId, file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/lessons/{lessonId}/resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listLessonResources(
            Authentication authentication,
            @PathVariable UUID lessonId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<ResourceResponse> response = resourceService.listLessonResources(actorId, lessonId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listCourseResources(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<ResourceResponse> response = resourceService.listCourseResources(actorId, courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/resources/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceDetail(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ResourceResponse response = resourceService.getResourceDetail(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/resources/{id}/download")
    public ResponseEntity<FileSystemResource> downloadResource(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ResourceService.ResourceDownload download = resourceService.downloadResource(actorId, id);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.getFilename())
                .build();

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.getFile());
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/resources/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> deleteResource(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ResourceResponse response = resourceService.deleteResource(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
