package com.educmind.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Module đang bật (Phase 1 của Spec v2): auth, schoolclass (lớp), course (khóa học/chủ đề/
 * bài học/mục tiêu/tài liệu), notification (chỉ đọc — bản ghi do trigger DB sinh).
 * Các module assignment/assessment/analytics/teacher-assistant đang chờ viết lại ở Phase 2-3
 * (xem docs/spec-v2/MIGRATION-STATUS.md), không nằm trong build.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.educmind.backend",
        "com.educmind.common",
        "com.educmind.auth",
        "com.educmind.schoolclass",
        "com.educmind.course",
        "com.educmind.notification"
})
@EntityScan(basePackages = {
        "com.educmind.auth.entity",
        "com.educmind.schoolclass.entity",
        "com.educmind.course.entity",
        "com.educmind.notification.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.educmind.auth.repository",
        "com.educmind.schoolclass.repository",
        "com.educmind.course.repository",
        "com.educmind.notification.repository"
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
