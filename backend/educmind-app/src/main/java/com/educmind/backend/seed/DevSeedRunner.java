package com.educmind.backend.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Chạy seed khi {@code educmind.seed.enabled=true} (biến môi trường EDUCMIND_SEED=true).
 * Dùng cho môi trường dev/demo; production để false.
 */
@Component
public class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final DevSeedService seedService;
    private final boolean enabled;
    private final String teacherEmail;
    private final String teacherPassword;
    private final String studentPassword;

    public DevSeedRunner(DevSeedService seedService,
                         @Value("${educmind.seed.enabled:false}") boolean enabled,
                         @Value("${educmind.seed.teacher-email:teacher@educmind.vn}") String teacherEmail,
                         @Value("${educmind.seed.teacher-password:Teacher@123}") String teacherPassword,
                         @Value("${educmind.seed.student-password:Student@123}") String studentPassword) {
        this.seedService = seedService;
        this.enabled = enabled;
        this.teacherEmail = teacherEmail;
        this.teacherPassword = teacherPassword;
        this.studentPassword = studentPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        String result = seedService.seed(teacherEmail, teacherPassword, studentPassword);
        if ("SEEDED".equals(result)) {
            log.info("Tài khoản demo — giáo viên: {} / {} · học sinh: student1..5@educmind.vn / {}",
                    teacherEmail, teacherPassword, studentPassword);
        }
    }
}
