package com.educmind.common.storage;

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
 * Lưu tệp tải lên trên đĩa cục bộ (dev). Khoá lưu trong DB có dạng
 * "&lt;prefix&gt;/&lt;uuid&gt;.&lt;ext&gt;" — không dùng tên tệp người dùng gửi để tránh path traversal.
 *
 * Dùng chung cho tài liệu bài học (module-course) và tệp đính kèm bài nộp (module-assignment).
 */
@Service
public class LocalFileStorage {

    private final Path basePath;

    public LocalFileStorage(
            @Value("${educmind.storage.local-base-path:./storage/resources}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    public String store(String prefix, MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        String key = prefix + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
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
            // Lỗi xóa tệp không được chặn nghiệp vụ xóa mềm bản ghi.
        }
    }

    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    public void validate(MultipartFile file, java.util.Set<String> allowedExtensions, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Tệp rỗng", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("FILE_TOO_LARGE",
                    "Tệp vượt quá " + (maxBytes / 1_048_576) + "MB", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE",
                    "Định dạng tệp không được hỗ trợ: ." + extension, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
