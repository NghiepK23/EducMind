package com.educmind.reports.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** Lưu file export lên filesystem local, tách khỏi storage của resources (module-course). */
@Service
public class ExportFileStorage {

    private final Path basePath;

    public ExportFileStorage(@Value("${educmind.storage.exports-path:./storage/exports}") String path) {
        this.basePath = Paths.get(path).toAbsolutePath().normalize();
    }

    public String store(UUID requestId, String extension, byte[] content) {
        String key = requestId + "." + extension;
        try {
            Files.createDirectories(basePath);
            Files.write(basePath.resolve(key), content);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu file export: " + e.getMessage(), e);
        }
    }

    public FileSystemResource load(String key) {
        return new FileSystemResource(basePath.resolve(key));
    }
}
