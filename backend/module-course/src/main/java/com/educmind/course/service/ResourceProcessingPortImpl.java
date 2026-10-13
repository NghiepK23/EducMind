package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.course.entity.Resource;
import com.educmind.course.repository.ResourceRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cài đặt {@link ResourceProcessingPort} cho module-rag (xem Module 6 — RAG).
 *
 * Các hàm markXxx không làm gì nếu resource không tồn tại hoặc đã bị xóa mềm,
 * vì worker nền có thể chạy xong sau khi giáo viên đã xóa tài liệu.
 */
@Service
public class ResourceProcessingPortImpl implements ResourceProcessingPort {

    private final ResourceRepository resourceRepository;
    private final ResourceStorageService resourceStorageService;

    public ResourceProcessingPortImpl(ResourceRepository resourceRepository,
                                      ResourceStorageService resourceStorageService) {
        this.resourceRepository = resourceRepository;
        this.resourceStorageService = resourceStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceInfo> findResource(UUID resourceId) {
        return resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
                .map(r -> new ResourceInfo(r.getId(), r.getCourseId(), r.getLessonId(),
                        r.getFilename(), r.getMimeType(), r.getSizeBytes()));
    }

    @Override
    public InputStream openContent(UUID resourceId) {
        Resource resource = resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
                .orElseThrow(() -> new BusinessException(
                        "RESOURCE_NOT_FOUND", "Không tìm thấy tài liệu", HttpStatus.NOT_FOUND));
        FileSystemResource file = resourceStorageService.load(resource.getStorageKey());
        if (!file.exists()) {
            throw new BusinessException(
                    "RESOURCE_FILE_MISSING", "File của tài liệu không còn trên storage",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new BusinessException(
                    "RESOURCE_READ_FAILED", "Không đọc được file của tài liệu",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findProcessingStatus(UUID resourceId) {
        return resourceRepository.findByIdAndDeletedAtIsNull(resourceId)
                .map(r -> r.getProcessingStatus().name());
    }

    @Override
    @Transactional
    public void markProcessing(UUID resourceId) {
        update(resourceId, Resource::markProcessing);
    }

    @Override
    @Transactional
    public void markReady(UUID resourceId) {
        update(resourceId, Resource::markReady);
    }

    @Override
    @Transactional
    public void markFailed(UUID resourceId) {
        update(resourceId, Resource::markFailed);
    }

    private void update(UUID resourceId, Consumer<Resource> change) {
        resourceRepository.findByIdAndDeletedAtIsNull(resourceId).ifPresent(resource -> {
            change.accept(resource);
            resourceRepository.save(resource);
        });
    }
}
