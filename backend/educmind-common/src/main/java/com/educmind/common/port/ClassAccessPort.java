package com.educmind.common.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port cho phép module khác tra cứu quan hệ lớp — học sinh mà KHÔNG phụ thuộc Maven
 * vào module-schoolclass (tránh circular dependency giữa auth và class).
 *
 * Mô hình Spec v2: lớp có ĐÚNG MỘT giáo viên phụ trách (classes.owner_teacher_id);
 * class_members chỉ chứa học sinh. Không còn "đồng giảng dạy" ở cấp lớp.
 */
public interface ClassAccessPort {

    /** true nếu lớp tồn tại (chưa xóa mềm) và teacherId là owner_teacher_id. */
    boolean isOwner(UUID teacherId, UUID classId);

    /** true nếu studentId đang là thành viên của lớp (chưa xóa mềm). */
    boolean isMember(UUID studentId, UUID classId);

    /** Thêm học sinh vào lớp; ném BusinessException nếu lớp không tồn tại/đã xóa. */
    void addStudent(UUID classId, UUID studentId);

    /** Danh sách id học sinh của lớp (rỗng nếu lớp không tồn tại). */
    List<UUID> findStudentIds(UUID classId);

    /** Danh sách lớp mà giáo viên phụ trách (chưa xóa mềm). */
    List<UUID> findClassIdsForTeacher(UUID teacherId);

    /** Danh sách lớp mà học sinh đang thuộc về (chưa xóa mềm). */
    List<UUID> findClassIdsForStudent(UUID studentId);

    /** owner_teacher_id của lớp chưa xóa mềm; empty nếu lớp không tồn tại. */
    Optional<UUID> findOwnerTeacherId(UUID classId);
}
