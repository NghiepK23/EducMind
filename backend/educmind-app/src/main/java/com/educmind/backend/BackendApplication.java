package com.educmind.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.educmind.backend",
        "com.educmind.common",
        "com.educmind.auth",
        "com.educmind.schoolclass",
        "com.educmind.course",
        "com.educmind.assignment",
        "com.educmind.assessment",
        "com.educmind.analytics",
        "com.educmind.notification",
        "com.educmind.reports",
        "com.educmind.teacherassistant"
})
@EntityScan(basePackages = {
        "com.educmind.auth.entity",
        "com.educmind.schoolclass.entity",
        "com.educmind.course.entity",
        "com.educmind.assignment.entity",
        "com.educmind.assessment.entity",
        "com.educmind.analytics.entity",
        "com.educmind.notification.entity",
        "com.educmind.reports.entity",
        "com.educmind.teacherassistant.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.educmind.auth.repository",
        "com.educmind.schoolclass.repository",
        "com.educmind.course.repository",
        "com.educmind.assignment.repository",
        "com.educmind.assessment.repository",
        "com.educmind.analytics.repository",
        "com.educmind.notification.repository",
        "com.educmind.reports.repository",
        "com.educmind.teacherassistant.repository"
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
