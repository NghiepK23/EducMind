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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Tài liệu học tập. Endpoint download trả file nhị phân (không bọc ApiResponse) — ngoại lệ
 * duy nhất của quy ước envelope, giống cách làm cũ.
 */
@RestController
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/lessons/{lessonId}/resources")
    public ResponseEntity<ApiResponse<ResourceResponse>> uploadToLesson(
            Authentication authentication,
            @PathVariable UUID lessonId,
            @RequestParam("file") MultipartFile file) {
        ResourceResponse response = resourceService.uploadToLesson(
                Actor.id(authentication), lessonId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/resources")
    public ResponseEntity<ApiResponse<ResourceResponse>> uploadToCourse(
            Authentication authentication,
            @PathVariable UUID courseId,
            @RequestParam("file") MultipartFile file) {
        ResourceResponse response = resourceService.uploadToCourse(
                Actor.id(authentication), courseId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/lessons/{lessonId}/resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listByLesson(
            Authentication authentication, @PathVariable UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.success(resourceService.listForLesson(
                Actor.id(authentication), Actor.role(authentication), lessonId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listByCourse(
            Authentication authentication, @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(resourceService.listForCourse(
                Actor.id(authentication), Actor.role(authentication), courseId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/resources/{id}/download")
    public ResponseEntity<FileSystemResource> download(Authentication authentication,
                                                       @PathVariable UUID id) {
        ResourceService.Download download = resourceService.download(
                Actor.id(authentication), Actor.role(authentication), id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (RuntimeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.file());
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/resources/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        resourceService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
