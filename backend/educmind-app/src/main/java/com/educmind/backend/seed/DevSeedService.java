package com.educmind.backend.seed;

import com.educmind.auth.entity.Role;
import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.course.entity.ContentStatus;
import com.educmind.course.entity.Course;
import com.educmind.course.entity.CourseTopic;
import com.educmind.course.entity.LearningObjective;
import com.educmind.course.entity.Lesson;
import com.educmind.course.repository.CourseRepository;
import com.educmind.course.repository.CourseTopicRepository;
import com.educmind.course.repository.LearningObjectiveRepository;
import com.educmind.course.repository.LessonRepository;
import com.educmind.schoolclass.entity.ClassMember;
import com.educmind.schoolclass.entity.SchoolClass;
import com.educmind.schoolclass.repository.ClassMemberRepository;
import com.educmind.schoolclass.repository.SchoolClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tạo dữ liệu mẫu để chạy được hệ thống ngay sau khi clone (repo trước đây không có seed nên
 * không thể đăng nhập lần đầu). Chạy một lần, idempotent: nếu email giáo viên đã tồn tại thì bỏ qua.
 */
@Service
public class DevSeedService {

    private static final Logger log = LoggerFactory.getLogger(DevSeedService.class);

    private final UserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final ClassMemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final CourseTopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LearningObjectiveRepository objectiveRepository;
    private final PasswordEncoder passwordEncoder;

    public DevSeedService(UserRepository userRepository,
                          SchoolClassRepository classRepository,
                          ClassMemberRepository memberRepository,
                          CourseRepository courseRepository,
                          CourseTopicRepository topicRepository,
                          LessonRepository lessonRepository,
                          LearningObjectiveRepository objectiveRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.objectiveRepository = objectiveRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String seed(String teacherEmail, String teacherPassword, String studentPassword) {
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(teacherEmail)) {
            log.info("Seed bỏ qua: {} đã tồn tại", teacherEmail);
            return "SKIPPED";
        }

        User teacher = userRepository.save(new User(
                teacherEmail, passwordEncoder.encode(teacherPassword), "Nguyễn Văn Giáo", Role.TEACHER));

        SchoolClass schoolClass = classRepository.save(new SchoolClass(
                "10A1", (short) 10, "2025-2026", teacher.getId()));

        List<String> studentNames = List.of(
                "Trần Thị An", "Lê Minh Bình", "Phạm Thu Chi", "Hoàng Gia Dũng", "Đỗ Khánh Em");
        for (int i = 0; i < studentNames.size(); i++) {
            User student = userRepository.save(new User(
                    "student" + (i + 1) + "@educmind.vn",
                    passwordEncoder.encode(studentPassword),
                    studentNames.get(i),
                    Role.STUDENT));
            memberRepository.save(new ClassMember(schoolClass.getId(), student.getId()));
        }

        Course course = courseRepository.save(new Course(
                schoolClass.getId(), "Tin học 10", "Khóa học Tin học 10 theo chương trình GDPT 2018."));

        CourseTopic topic1 = new CourseTopic(course.getId(),
                "Chủ đề 1 — Máy tính và xã hội tri thức",
                "Thông tin, dữ liệu và cách biểu diễn thông tin trong máy tính.", 1);
        topic1.publish(teacher.getId());
        topic1 = topicRepository.save(topic1);

        CourseTopic topic2 = topicRepository.save(new CourseTopic(course.getId(),
                "Chủ đề 2 — Mạng máy tính và Internet",
                "Khái niệm mạng, giao thức và dịch vụ Internet.", 2));

        Lesson lesson1 = new Lesson(course.getId(), topic1.getId(),
                "Bài 1 — Thông tin và dữ liệu",
                "Phân biệt thông tin, dữ liệu; vai trò của thông tin trong đời sống.");
        lesson1.publish(teacher.getId());
        lesson1 = lessonRepository.save(lesson1);

        objectiveRepository.save(new LearningObjective(lesson1.getId(), "LO01",
                "Nêu được khái niệm thông tin và dữ liệu."));
        objectiveRepository.save(new LearningObjective(lesson1.getId(), "LO02",
                "Phân biệt được thông tin với dữ liệu thông qua ví dụ."));

        Lesson lesson2 = lessonRepository.save(new Lesson(course.getId(), topic1.getId(),
                "Bài 2 — Biểu diễn dữ liệu trong máy tính",
                "Chuyển đổi giữa các hệ đếm và biểu diễn văn bản, hình ảnh, âm thanh."));
        objectiveRepository.save(new LearningObjective(lesson2.getId(), "LO01",
                "Chuyển đổi được số giữa hệ nhị phân và hệ thập phân."));

        Lesson lesson3 = lessonRepository.save(new Lesson(course.getId(), topic2.getId(),
                "Bài 3 — Mạng máy tính",
                "Thiết bị mạng, giao thức và mô hình kết nối."));
        objectiveRepository.save(new LearningObjective(lesson3.getId(), "LO01",
                "Kể tên được các thành phần cơ bản của một mạng máy tính."));

        log.info("Seed xong: lớp {} ({}), khóa học '{}' ({} chủ đề, {} bài học). Trạng thái chủ đề 2: {}",
                schoolClass.getName(), schoolClass.getId(), course.getTitle(),
                2, 3, ContentStatus.DRAFT);

        return "SEEDED";
    }
}
