package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Ghi/đọc file tài liệu trên đĩa cục bộ (dev). Đường dẫn lưu trong resources.file_url
 * là khóa tương đối dạng "<courseId>/<uuid>.<ext>" — không bao giờ dùng tên file
 * người dùng gửi để tránh path traversal.
 */
@Service
public class ResourceStorageService {

    private final Path basePath;

    public ResourceStorageService(
            @Value("${educmind.storage.local-base-path:./storage/resources}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    public String store(UUID courseId, MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        String key = courseId + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = basePath.resolve(key).normalize();

        if (!target.startsWith(basePath)) {
            throw new BusinessException("INVALID_FILE_PATH", "Đường dẫn tệp không hợp lệ", HttpStatus.BAD_REQUEST);
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException("STORAGE_ERROR",
                    "Không lưu được tệp: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return key;
    }

    public FileSystemResource load(String key) {
        Path file = basePath.resolve(key).normalize();
        if (!file.startsWith(basePath) || !Files.exists(file)) {
            throw new BusinessException("RESOURCE_FILE_MISSING",
                    "Tệp không còn trên máy chủ", HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(file);
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(basePath.resolve(key).normalize());
        } catch (IOException ignored) {
            // Xóa file lỗi không nên chặn nghiệp vụ xóa mềm bản ghi.
        }
    }

    static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}
