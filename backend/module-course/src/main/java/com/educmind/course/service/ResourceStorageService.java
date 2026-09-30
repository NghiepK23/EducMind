package com.educmind.course.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Chịu trách nhiệm duy nhất: đọc/ghi file vật lý lên storage.
 *
 * Hiện tại dùng filesystem local (chưa có MinIO/S3 — xem Checkpoint #5, mục 8).
 * Khi có Object Storage thật, chỉ cần thay nội dung bên trong class này
 * (store/load), KHÔNG cần sửa ResourceService hay bất kỳ chỗ nào khác — đây là
 * lý do tách riêng thành 1 service độc lập thay vì viết thẳng logic filesystem
 * trong ResourceService.
 */
@Service
public class ResourceStorageService {

    private final Path basePath;

    public ResourceStorageService(@Value("${educmind.storage.local-base-path}") String localBasePath) {
        this.basePath = Paths.get(localBasePath).toAbsolutePath().normalize();
    }

    /**
     * Ghi file vào storage, trả về storageKey (đường dẫn tương đối, dùng để load lại sau này)
     * và số byte thực tế đã ghi (đo trực tiếp khi copy, không tin vào kích thước client khai báo).
     */
    public StoredFile store(UUID courseId, String originalFilename, InputStream content) {
        String storedFilename = UUID.randomUUID() + "_" + sanitizeFilename(originalFilename);
        String relativeKey = courseId + "/" + storedFilename;

        Path targetDir = basePath.resolve(courseId.toString());
        Path targetFile = basePath.resolve(relativeKey);

        try {
            Files.createDirectories(targetDir);
            long bytesWritten = Files.copy(content, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(relativeKey, bytesWritten);
        } catch (IOException e) {
            throw new ResourceStorageException("Không thể lưu file vào storage: " + e.getMessage(), e);
        }
    }

    /**
     * Đọc lại file theo storageKey đã lưu — dùng cho endpoint download.
     */
    public FileSystemResource load(String storageKey) {
        Path file = basePath.resolve(storageKey);
        return new FileSystemResource(file);
    }

    /**
     * Loại bỏ ký tự path separator khỏi tên file gốc để tránh path traversal
     * (vd người dùng đặt tên file "../../etc/passwd").
     */
    private String sanitizeFilename(String originalFilename) {
        String name = Paths.get(originalFilename).getFileName().toString();
        return name.replaceAll("[\\\\/]", "_");
    }

    public static class StoredFile {
        private final String storageKey;
        private final long sizeBytes;

        public StoredFile(String storageKey, long sizeBytes) {
            this.storageKey = storageKey;
            this.sizeBytes = sizeBytes;
        }

        public String getStorageKey() {
            return storageKey;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }
    }

    public static class ResourceStorageException extends RuntimeException {
        public ResourceStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
