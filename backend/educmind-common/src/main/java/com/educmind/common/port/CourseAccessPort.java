package com.educmind.common.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (theo kiến trúc Hexagonal/Ports-and-Adapters) cho phép các module khác
 * (hiện tại: module-assignment, module-assessment, và tương lai module-lesson)
 * truy vấn quyền sở hữu/đồng giảng dạy Course mà KHÔNG cần khai báo Maven
 * dependency trực tiếp tới module-course.
 *
 * [CẬP NHẬT — THAY ĐỔI KIẾN TRÚC LỚN, xem CourseAccessPortImpl] Course không còn
 * N-N với Class qua bảng course_class nữa. Mỗi course tự có danh sách thành viên
 * riêng (course_members) — "đồng giảng dạy" nghĩa là có 1 dòng ACTIVE (leftAt IS
 * NULL) trong course_members với memberRole='TEACHER' cho ĐÚNG course đó; "học
 * sinh đã enroll" nghĩa là có 1 dòng ACTIVE với memberRole='STUDENT'. Không còn
 * khái niệm "active-link qua class" — implementation (CourseAccessPortImpl, ở
 * module-course) đã viết lại theo đúng model này.
 *
 * Thiết kế song song với ClassAccessPort (xem file đó để hiểu đầy đủ lý do cần
 * tách port thay vì gọi thẳng Repository/Service của module khác). Điểm khác biệt
 * duy nhất: ClassAccessPort có thêm addMember() vì việc thêm thành viên Class có
 * luồng nghiệp vụ riêng (invite code — Module 1); việc thêm course_members hiện
 * tại chỉ có API riêng ở Module 3 (do CourseService xử lý trực tiếp), các module
 * dùng port này không cần thao tác đó, nên KHÔNG có method addMember ở đây.
 *
 * Tham số/kết quả CHỈ dùng kiểu nguyên thủy (UUID, boolean, Optional, List) — không
 * dùng bất kỳ Entity nào của module-course (vd Course), để giữ educmind-common
 * hoàn toàn trung lập, không "biết" gì về module-course.
 */
public interface CourseAccessPort {

    /**
     * true nếu courseId tồn tại (chưa xóa) VÀ actorId là owner (created_by)
     * HOẶC đồng giảng dạy (có 1 dòng ACTIVE trong course_members với
     * memberRole='TEACHER' cho đúng courseId đó). false nếu course không tồn
     * tại hoặc actor không có quyền quản lý.
     *
     * Dùng cho: Module 4 kiểm tra quyền tạo/sửa/xóa assignment, question bank
     * (Module04_Assignment_Assessment_FINAL.md, mục D.6/D.8, E.6/E.9 —
     * "TEACHER owner hoặc đồng giảng dạy course").
     */
    boolean isOwnerOrCoTeacher(UUID actorId, UUID courseId);

    /**
     * true nếu courseId tồn tại (chưa xóa) VÀ actorId CHÍNH LÀ owner gốc
     * (created_by) — KHÔNG tính đồng giảng dạy, khác với isOwnerOrCoTeacher().
     *
     * Dùng cho: các hành động chỉ owner gốc mới được làm, ví dụ Module 4
     * chấm điểm assignment/assessment (Module04_Assignment_Assessment_FINAL.md,
     * đã chốt: đồng giảng dạy được tạo/sửa ngang owner NHƯNG không được chấm
     * điểm, chỉ owner gốc mới chấm được).
     */
    boolean isOwner(UUID actorId, UUID courseId);

    /**
     * true nếu studentId đang có 1 dòng ACTIVE trong course_members với
     * memberRole='STUDENT' cho đúng courseId đó. false nếu course không tồn
     * tại hoặc student không phải thành viên active của course đó.
     *
     * Dùng cho: Module 4 lọc assignment/assessment PUBLISHED mà học sinh được
     * phép thấy/nộp bài (chỉ học sinh đã enroll vào course mới thấy).
     */
    boolean isStudentEnrolled(UUID studentId, UUID courseId);

    /**
     * Trả về school_id của course, hoặc Optional.empty() nếu courseId không
     * tồn tại/đã xóa.
     *
     * Dùng cho: validate course tồn tại trước khi tạo assignment/question,
     * và kiểm tra phạm vi SCHOOL_ADMIN (chỉ xem course trong trường mình).
     */
    Optional<UUID> findSchoolIdByCourseId(UUID courseId);

    /**
     * Danh sách courseId mà teacherId là owner (created_by) HOẶC đồng giảng dạy
     * (có 1 dòng ACTIVE trong course_members với memberRole='TEACHER' cho course
     * đó), loại trừ course đã soft-delete.
     *
     * Dùng cho: Module 4 liệt kê assignment/assessment theo course mà giáo viên
     * đang quản lý, mà không cần expose Entity Course ra ngoài module.
     */
    List<UUID> findAccessibleCourseIdsForTeacher(UUID teacherId);

    /**
     * source_class_id của học sinh trong course (lớp hành chính nguồn khi thêm vào
     * course). Không lọc left_at. Optional.empty() nếu không có dòng STUDENT
     * hoặc source_class_id là NULL.
     *
     * Dùng cho: Module 5 điền learning_evidence.class_id.
     */
    Optional<UUID> findSourceClassId(UUID courseId, UUID studentId);

    /**
     * Danh sách studentId đang ACTIVE (left_at IS NULL, memberRole='STUDENT') trong
     * course, với source_class_id = classId. Rỗng nếu không có học sinh nào khớp
     * (kể cả khi course/class không tồn tại — caller tự kiểm tra quyền trước).
     *
     * Dùng cho: Module 5, lọc knowledge gap / mastery theo lớp (class_id) ở API giáo viên.
     */
    List<UUID> findActiveStudentIdsByCourseAndSourceClass(UUID courseId, UUID classId);

    /**
     * Danh sách courseId (chưa soft-delete) thuộc schoolId. Rỗng nếu trường không có course nào.
     *
     * Dùng cho: Module 5, API SCHOOL_ADMIN xem tổng hợp analytics theo course trong trường mình.
     */
    List<UUID> findCourseIdsBySchoolId(UUID schoolId);
}
