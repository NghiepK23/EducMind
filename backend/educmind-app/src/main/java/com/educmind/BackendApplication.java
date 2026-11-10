package com.educmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động. Package gốc là {@code com.educmind} nên Spring tự quét component/entity/repository
 * của mọi module đang bật: auth, schoolclass, course, notification, common, backend.
 *
 * Phase 1 của Spec v2. Các module assignment/assessment/analytics/teacher-assistant đang chờ
 * viết lại ở Phase 2-3 (xem docs/spec-v2/MIGRATION-STATUS.md) và không nằm trong build.
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
