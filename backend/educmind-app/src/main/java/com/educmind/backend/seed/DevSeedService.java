package com.educmind.backend.seed;

import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentObjective;
import com.educmind.assignment.entity.AssignmentSubmission;
import com.educmind.assignment.entity.SubmissionStatus;
import com.educmind.assignment.repository.AssignmentObjectiveRepository;
import com.educmind.assignment.repository.AssignmentRepository;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentAttempt;
import com.educmind.assessment.entity.AssessmentQuestion;
import com.educmind.assessment.entity.DifficultyLevel;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionObjective;
import com.educmind.assessment.entity.QuestionType;
import com.educmind.assessment.entity.SubmissionAnswer;
import com.educmind.assessment.repository.AssessmentAttemptRepository;
import com.educmind.assessment.repository.AssessmentQuestionRepository;
import com.educmind.assessment.repository.AssessmentRepository;
import com.educmind.assessment.repository.QuestionObjectiveRepository;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.assessment.repository.SubmissionAnswerRepository;
import com.educmind.auth.entity.Role;
import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.port.LearningEvidencePort;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
    private final AssignmentRepository assignmentRepository;
    private final AssignmentObjectiveRepository assignmentObjectiveRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionObjectiveRepository questionObjectiveRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final LearningEvidencePort evidencePort;
    private final PasswordEncoder passwordEncoder;

    public DevSeedService(UserRepository userRepository,
                          SchoolClassRepository classRepository,
                          ClassMemberRepository memberRepository,
                          CourseRepository courseRepository,
                          CourseTopicRepository topicRepository,
                          LessonRepository lessonRepository,
                          LearningObjectiveRepository objectiveRepository,
                          AssignmentRepository assignmentRepository,
                          AssignmentObjectiveRepository assignmentObjectiveRepository,
                          AssignmentSubmissionRepository submissionRepository,
                          QuestionRepository questionRepository,
                          QuestionObjectiveRepository questionObjectiveRepository,
                          AssessmentRepository assessmentRepository,
                          AssessmentQuestionRepository assessmentQuestionRepository,
                          AssessmentAttemptRepository attemptRepository,
                          SubmissionAnswerRepository answerRepository,
                          LearningEvidencePort evidencePort,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.objectiveRepository = objectiveRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentObjectiveRepository = assignmentObjectiveRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.questionObjectiveRepository = questionObjectiveRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.evidencePort = evidencePort;
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
        User[] students = new User[studentNames.size()];
        for (int i = 0; i < studentNames.size(); i++) {
            User student = userRepository.save(new User(
                    "student" + (i + 1) + "@educmind.vn",
                    passwordEncoder.encode(studentPassword),
                    studentNames.get(i),
                    Role.STUDENT));
            memberRepository.save(new ClassMember(schoolClass.getId(), student.getId()));
            students[i] = student;
        }
        User student1 = students[0]; // giỏi — có bằng chứng ở mọi mục tiêu
        User student2 = students[1]; // yếu MỘT mục tiêu (LO01 bài 1)
        // student3 (students[2]) cố ý KHÔNG làm bài → "Chưa có dữ liệu" (AC-T07.2)

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

        LearningObjective lo01 = objectiveRepository.save(new LearningObjective(lesson1.getId(), "LO01",
                "Nêu được khái niệm thông tin và dữ liệu."));
        LearningObjective lo02 = objectiveRepository.save(new LearningObjective(lesson1.getId(), "LO02",
                "Phân biệt được thông tin với dữ liệu thông qua ví dụ."));

        Lesson lesson2 = lessonRepository.save(new Lesson(course.getId(), topic1.getId(),
                "Bài 2 — Biểu diễn dữ liệu trong máy tính",
                "Chuyển đổi giữa các hệ đếm và biểu diễn văn bản, hình ảnh, âm thanh."));
        LearningObjective loLesson2 = objectiveRepository.save(new LearningObjective(lesson2.getId(), "LO01",
                "Chuyển đổi được số giữa hệ nhị phân và hệ thập phân."));

        Lesson lesson3 = lessonRepository.save(new Lesson(course.getId(), topic2.getId(),
                "Bài 3 — Mạng máy tính",
                "Thiết bị mạng, giao thức và mô hình kết nối."));
        LearningObjective loLesson3 = objectiveRepository.save(new LearningObjective(lesson3.getId(), "LO01",
                "Kể tên được các thành phần cơ bản của một mạng máy tính."));

        // Bài tập mẫu để thử luồng US-T03/T04, US-S02/S04/S07 ngay sau khi seed.
        OffsetDateTime now = OffsetDateTime.now();
        Assignment assignment = new Assignment(course.getId(),
                "Bài tập 1 — Thông tin và dữ liệu",
                "Trình bày sự khác nhau giữa thông tin và dữ liệu, lấy 3 ví dụ thực tế.",
                now.plusDays(7), new BigDecimal("10.00"), true, now.plusDays(10));
        assignment.publish(teacher.getId());
        assignment = assignmentRepository.save(assignment);

        assignmentObjectiveRepository.save(new AssignmentObjective(assignment.getId(), lo01.getId()));
        assignmentObjectiveRepository.save(new AssignmentObjective(assignment.getId(), lo02.getId()));

        // ------------------------------------------------------------------
        // Phase 3A — dữ liệu mastery / lỗ hổng / gợi ý (US-T07/T08, US-S05/S06)
        // ------------------------------------------------------------------
        // 6 câu hỏi: Q1–Q4 vào bài kiểm tra (tổng 10 điểm), Q5–Q6 chỉ trong ngân hàng.
        Question q1 = questionRepository.saveAndFlush(new Question(course.getId(),
                "Dữ liệu sau khi được xử lý và diễn giải thành ý nghĩa được gọi là gì?",
                QuestionType.SINGLE_CHOICE, DifficultyLevel.EASY, "NHỚ",
                "[\"Thông tin\",\"Tín hiệu\",\"Mã nguồn\",\"Nhiễu\"]", "[0]", teacher.getId()));
        Question q2 = questionRepository.saveAndFlush(new Question(course.getId(),
                "Phân biệt thông tin với dữ liệu dựa trên tiêu chí nào?",
                QuestionType.SINGLE_CHOICE, DifficultyLevel.MEDIUM, "HIỂU",
                "[\"Mức độ xử lý\",\"Độ dài chuỗi\",\"Thời điểm tạo\",\"Số lượng\"]", "[0]", teacher.getId()));
        Question q3 = questionRepository.saveAndFlush(new Question(course.getId(),
                "Những yếu tố nào dưới đây là thông tin? (chọn tất đáp án đúng)",
                QuestionType.MULTIPLE_CHOICE, DifficultyLevel.MEDIUM, "VẬN DỤNG",
                "[\"Nhiệt độ 30 độ C đo được\",\"Chuỗi 011011 không ngữ cảnh\",\"Tin nhắn 'Họp lúc 9 giờ'\",\"File .exe ngẫu nhiên\"]",
                "[0,2]", teacher.getId()));
        Question q4 = questionRepository.saveAndFlush(new Question(course.getId(),
                "Thông tin là dữ liệu đã được xử lý và mang ý nghĩa đối với người nhận.",
                QuestionType.TRUE_FALSE, DifficultyLevel.EASY, "HIỂU",
                "[\"Đúng\",\"Sai\"]", "[0]", teacher.getId()));
        Question q5 = questionRepository.saveAndFlush(new Question(course.getId(),
                "Số 13 (thập phân) viết trong hệ nhị phân là bao nhiêu?",
                QuestionType.SHORT_ANSWER, DifficultyLevel.HARD, "VẬN DỤNG",
                null, "\"1011\"", teacher.getId()));
        Question q6 = questionRepository.saveAndFlush(new Question(course.getId(),
                "Trình bày vai trò của giao thức TCP/IP trong mạng máy tính.",
                QuestionType.ESSAY, DifficultyLevel.HARD, "PHÂN TÍCH",
                null, null, teacher.getId()));

        // Trigger fn_check_question_objective_course đọc bảng questions → questions phải flush trước.
        questionObjectiveRepository.saveAndFlush(new QuestionObjective(q1.getId(), lo01.getId()));
        questionObjectiveRepository.saveAndFlush(new QuestionObjective(q2.getId(), lo01.getId()));
        questionObjectiveRepository.saveAndFlush(new QuestionObjective(q3.getId(), lo02.getId()));
        questionObjectiveRepository.saveAndFlush(new QuestionObjective(q4.getId(), lo02.getId()));
        questionObjectiveRepository.saveAndFlush(new QuestionObjective(q5.getId(), loLesson2.getId()));
        questionObjectiveRepository.saveAndFlush(new QuestionObjective(q6.getId(), loLesson3.getId()));

        // Bài kiểm tra publish trước khi save (như bài tập 1): 4 câu, tổng 10 điểm, 3 lượt.
        Assessment assessment = new Assessment(course.getId(),
                "Kiểm tra 30 phút — Thông tin và dữ liệu",
                30, 3, now.minusHours(1), now.plusDays(7));
        assessment.publish(teacher.getId());
        assessment = assessmentRepository.saveAndFlush(assessment);

        assessmentQuestionRepository.saveAndFlush(
                new AssessmentQuestion(assessment.getId(), q1.getId(), new BigDecimal("2.00")));
        assessmentQuestionRepository.saveAndFlush(
                new AssessmentQuestion(assessment.getId(), q2.getId(), new BigDecimal("3.00")));
        assessmentQuestionRepository.saveAndFlush(
                new AssessmentQuestion(assessment.getId(), q3.getId(), new BigDecimal("2.00")));
        assessmentQuestionRepository.saveAndFlush(
                new AssessmentQuestion(assessment.getId(), q4.getId(), new BigDecimal("3.00")));

        // Bài tập 2 phạm vi bài 2 (bài học đang nháp) — nguồn bằng chứng LO01 bài 2 cho HS1.
        Assignment assignment2 = new Assignment(course.getId(),
                "Bài tập 2 — Chuyển đổi dữ liệu",
                "Chuyển 5 số thập phân sang nhị phân và giải thích cách đọc kết quả.",
                now.plusDays(14), new BigDecimal("10.00"), true, now.plusDays(17));
        assignment2.update(null, null, null, null, null, null, null, lesson2.getId(), teacher.getId());
        assignment2.publish(teacher.getId());
        assignment2 = assignmentRepository.saveAndFlush(assignment2);
        assignmentObjectiveRepository.saveAndFlush(new AssignmentObjective(assignment2.getId(), loLesson2.getId()));

        // --- Bài nộp có điểm → bằng chứng (record refresh mastery/gap/rec cùng transaction) ---
        AssignmentSubmission sub1 = new AssignmentSubmission(assignment.getId(), student1.getId(),
                "Thông tin là dữ liệu đã được xử lý và mang ý nghĩa; ví dụ: nhiệt độ, bản tin, tin nhắn.",
                null, SubmissionStatus.SUBMITTED);
        sub1 = submissionRepository.saveAndFlush(sub1);
        sub1.grade(new BigDecimal("9.50"), "Rất tốt, ví dụ sinh động.", teacher.getId(), OffsetDateTime.now());
        sub1 = submissionRepository.saveAndFlush(sub1);
        evidencePort.record(student1.getId(), lo01.getId(), LearningEvidencePort.SourceType.ASSIGNMENT, sub1.getId(), 0.95);
        evidencePort.record(student1.getId(), lo02.getId(), LearningEvidencePort.SourceType.ASSIGNMENT, sub1.getId(), 0.95);

        AssignmentSubmission sub2 = new AssignmentSubmission(assignment.getId(), student2.getId(),
                "Em chưa phân biệt rõ, chắc là dữ liệu là con số còn thông tin là chữ.",
                null, SubmissionStatus.SUBMITTED);
        sub2 = submissionRepository.saveAndFlush(sub2);
        sub2.grade(new BigDecimal("3.00"), "Cần ôn lại phần phân biệt thông tin và dữ liệu.",
                teacher.getId(), OffsetDateTime.now());
        sub2 = submissionRepository.saveAndFlush(sub2);
        evidencePort.record(student2.getId(), lo01.getId(), LearningEvidencePort.SourceType.ASSIGNMENT, sub2.getId(), 0.30);
        evidencePort.record(student2.getId(), lo02.getId(), LearningEvidencePort.SourceType.ASSIGNMENT, sub2.getId(), 0.30);

        AssignmentSubmission sub3 = new AssignmentSubmission(assignment2.getId(), student1.getId(),
                "13 = 8 + 4 + 1 nên 13(10) = 1011(2); đọc từ bit trọng số cao xuống thấp.",
                null, SubmissionStatus.SUBMITTED);
        sub3 = submissionRepository.saveAndFlush(sub3);
        sub3.grade(new BigDecimal("9.50"), "Chính xác và giải thích rõ.", teacher.getId(), OffsetDateTime.now());
        sub3 = submissionRepository.saveAndFlush(sub3);
        evidencePort.record(student1.getId(), loLesson2.getId(),
                LearningEvidencePort.SourceType.ASSIGNMENT, sub3.getId(), 0.95);

        // --- Lượt làm bài kiểm tra (IN_PROGRESS → GRADED trong đúng 1 UPDATE) ---
        AssessmentAttempt attempt1 = new AssessmentAttempt(assessment.getId(), student1.getId(), 1);
        attempt1 = attemptRepository.saveAndFlush(attempt1);
        SubmissionAnswer ans11 = new SubmissionAnswer(attempt1.getId(), q1.getId(), "[0]");
        ans11.autoGrade(new BigDecimal("2.00"));
        answerRepository.saveAndFlush(ans11);
        SubmissionAnswer ans12 = new SubmissionAnswer(attempt1.getId(), q2.getId(), "[0]");
        ans12.autoGrade(new BigDecimal("3.00"));
        answerRepository.saveAndFlush(ans12);
        SubmissionAnswer ans13 = new SubmissionAnswer(attempt1.getId(), q3.getId(), "[0,2]");
        ans13.autoGrade(new BigDecimal("2.00"));
        answerRepository.saveAndFlush(ans13);
        SubmissionAnswer ans14 = new SubmissionAnswer(attempt1.getId(), q4.getId(), "[0]");
        ans14.autoGrade(new BigDecimal("3.00"));
        answerRepository.saveAndFlush(ans14);
        attempt1.markSubmitted(OffsetDateTime.now());
        attempt1.markGraded(new BigDecimal("10.00"), OffsetDateTime.now());
        attempt1 = attemptRepository.saveAndFlush(attempt1);
        evidencePort.recordHighest(student1.getId(), lo01.getId(),
                LearningEvidencePort.SourceType.ASSESSMENT, assessment.getId(), 1.0);
        evidencePort.recordHighest(student1.getId(), lo02.getId(),
                LearningEvidencePort.SourceType.ASSESSMENT, assessment.getId(), 1.0);

        AssessmentAttempt attempt2 = new AssessmentAttempt(assessment.getId(), student2.getId(), 1);
        attempt2 = attemptRepository.saveAndFlush(attempt2);
        SubmissionAnswer ans21 = new SubmissionAnswer(attempt2.getId(), q1.getId(), "[1]");
        ans21.autoGrade(BigDecimal.ZERO);
        answerRepository.saveAndFlush(ans21);
        SubmissionAnswer ans22 = new SubmissionAnswer(attempt2.getId(), q2.getId(), "[1]");
        ans22.autoGrade(BigDecimal.ZERO);
        answerRepository.saveAndFlush(ans22);
        SubmissionAnswer ans23 = new SubmissionAnswer(attempt2.getId(), q3.getId(), "[0,2]");
        ans23.autoGrade(new BigDecimal("2.00"));
        answerRepository.saveAndFlush(ans23);
        SubmissionAnswer ans24 = new SubmissionAnswer(attempt2.getId(), q4.getId(), "[0]");
        ans24.autoGrade(new BigDecimal("3.00"));
        answerRepository.saveAndFlush(ans24);
        attempt2.markSubmitted(OffsetDateTime.now());
        attempt2.markGraded(new BigDecimal("5.00"), OffsetDateTime.now());
        attempt2 = attemptRepository.saveAndFlush(attempt2);
        evidencePort.recordHighest(student2.getId(), lo01.getId(),
                LearningEvidencePort.SourceType.ASSESSMENT, assessment.getId(), 0.0);
        evidencePort.recordHighest(student2.getId(), lo02.getId(),
                LearningEvidencePort.SourceType.ASSESSMENT, assessment.getId(), 1.0);

        log.info("Seed xong: lớp {} ({}), khóa học '{}' ({} chủ đề, {} bài học), bài tập '{}' + '{}', "
                        + "bài kiểm tra '{}' ({} câu, {} điểm), bài nộp + lượt làm có điểm → mastery/gap/rec sẵn sàng. "
                        + "Trạng thái chủ đề 2: {}",
                schoolClass.getName(), schoolClass.getId(), course.getTitle(),
                2, 3, assignment.getTitle(), assignment2.getTitle(), assessment.getTitle(), 6, 10,
                ContentStatus.DRAFT);

        return "SEEDED";
    }
}
