package com.educmind.common.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (theo kiến trúc Hexagonal/Ports-and-Adapters) cho phép các module khác
 * (hiện tại: module-auth, module-course) truy vấn/thao tác dữ liệu class_members
 * và classes mà KHÔNG cần khai báo Maven dependency trực tiếp tới module-schoolclass.
 *
 * [Giải thích cho người mới] Vì sao cần interface này thay vì gọi thẳng
 * SchoolClassRepository/ClassMemberRepository như trước đây?
 *
 * Nếu module-auth phụ thuộc Maven trực tiếp vào module-schoolclass, và sau này
 * module-schoolclass lại cần gọi ngược lại module-auth (ví dụ để kiểm tra role
 * của user khi tạo Class) — Maven sẽ báo lỗi circular dependency, chặn build
 * hoàn toàn. Interface này được định nghĩa ở educmind-common (module trung lập
 * mà MỌI module khác đều phụ thuộc), rồi module-schoolclass sẽ là nơi
 * IMPLEMENT interface này (@Component), còn module-auth/module-course chỉ cần
 * INJECT interface qua Spring (không cần biết implementation nằm ở đâu). Vì đây
 * là modular monolith (1 process, 1 Spring context khi chạy educmind-app), Spring
 * tự động "nối dây" đúng bean lúc runtime — tốc độ như gọi hàm bình thường,
 * không qua REST nội bộ.
 *
 * Tham số/kết quả CHỈ dùng kiểu nguyên thủy (UUID, boolean, Optional) — không
 * dùng bất kỳ Entity/Enum nào của module-schoolclass (vd MemberRole), để giữ
 * educmind-common hoàn toàn trung lập, không "biết" gì về module-schoolclass.
 */
public interface ClassAccessPort {

    /**
     * true nếu classId tồn tại VÀ teacherId là owner_teacher_id của lớp đó,
     * HOẶC teacherId có mặt trong class_members của lớp đó với vai trò giáo viên
     * (chưa rời lớp — left_at IS NULL). false nếu class không tồn tại hoặc
     * teacherId không có quan hệ gì với lớp.
     *
     * Dùng cho: kiểm tra quyền tạo/thu hồi invite code (Module 1, mục A.5.3),
     * và định nghĩa "đồng giảng dạy" ở cấp Course (Module 3, mục C.9).
     */
    boolean isOwnerOrCoTeacher(UUID teacherId, UUID classId);

    /**
     * true nếu userId đang là thành viên active (left_at IS NULL) của classId,
     * KHÔNG phân biệt member_role (TEACHER hay STUDENT) — bao gồm cả trường hợp
     * userId là owner_teacher_id (owner không có dòng class_members riêng, nhưng
     * vẫn được coi là "active member" cho mục đích kiểm tra chung này).
     * false nếu class không tồn tại hoặc userId không có quan hệ active nào.
     *
     * Dùng cho: kiểm tra STUDENT có quyền xem 1 Course hay không (Module 3, mục
     * C.9) — STUDENT chỉ xem được course nếu thuộc 1 class đã link course đó,
     * tức cần biết "user này còn active trong class X không" mà không quan tâm
     * vai trò cụ thể.
     */
    boolean isActiveMember(UUID userId, UUID classId);

    /**
     * Thêm 1 thành viên vào lớp (tạo dòng class_members mới).
     * isTeacherRole = true -> member_role=TEACHER; false -> member_role=STUDENT.
     *
     * Dùng cho: SCHOOL_ADMIN tạo tài khoản kèm class_id (UserService.createUser),
     * và học sinh/giáo viên redeem invite code (AuthService.register).
     */
    void addMember(UUID classId, UUID userId, boolean isTeacherRole);

    /**
     * Trả về school_id của lớp, hoặc Optional.empty() nếu classId không tồn tại.
     * Dùng cho: validate classId tồn tại + lấy schoolId khi tạo invite code
     * (InviteCode.schoolId suy ra từ classId theo đúng spec A.4).
     */
    Optional<UUID> findSchoolIdByClassId(UUID classId);

    /**
     * Danh sách classId mà teacherId là owner_teacher_id HOẶC đồng giảng dạy
     * (có mặt trong class_members, chưa rời lớp), loại trừ lớp đã soft-delete.
     * Dùng cho: Module 3, xác định course nào teacher đồng giảng dạy (qua join
     * với course_class), mà không cần expose Entity SchoolClass ra ngoài module.
     */
    java.util.List<UUID> findAccessibleClassIdsForTeacher(UUID teacherId);

    /**
     * school_id của lớp CHƯA bị soft-delete (deleted_at IS NULL); Optional.empty() nếu lớp
     * không tồn tại hoặc đã bị xóa mềm. Khác findSchoolIdByClassId (không lọc deleted_at).
     * Dùng cho: SYSTEM_ANNOUNCEMENT scope=CLASS (Module 10, J.6.3).
     */
    Optional<UUID> findSchoolIdOfActiveClass(UUID classId);

    /**
     * userId của mọi thành viên đang active (left_at IS NULL, cả TEACHER lẫn STUDENT)
     * CỘNG owner_teacher_id của lớp (owner không có dòng class_members). Không trùng lặp.
     * Rỗng nếu lớp không tồn tại hoặc đã soft-delete.
     */
    java.util.List<UUID> findActiveMemberUserIds(UUID classId);

}
