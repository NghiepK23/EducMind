package com.educmind.common.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port cho phép các module khác (hiện tại: module-schoolclass) tra cứu thông tin cơ bản
 * của 1 user mà KHÔNG cần khai báo Maven dependency trực tiếp tới module-auth.
 *
 * [Giải thích cho người mới] Đây là port theo CHIỀU NGƯỢC LẠI với ClassAccessPort
 * (ClassAccessPort: module-auth gọi sang module-schoolclass; UserLookupPort:
 * module-schoolclass gọi sang module-auth). Cả 2 port cùng nằm ở educmind-common —
 * đây chính là kịch bản "symmetric need" đã dự đoán từ trước: Class CRUD (module-schoolclass)
 * cần biết role/school của 1 user (vd: validate ownerTeacherId khi SCHOOL_ADMIN tạo lớp hộ
 * giáo viên) — nhu cầu này không tồn tại khi chỉ làm Module 1 Phần A (Auth).
 *
 * UserSummary dùng String cho role (không dùng Role enum của module-auth) để giữ
 * educmind-common trung lập, theo đúng quy ước đã áp dụng ở ClassAccessPort.
 */
public interface UserLookupPort {

    /**
     * Thông tin tóm tắt của 1 user, đủ dùng cho các validate cross-module phổ biến
     * (đúng role? đúng trường? còn active?) mà không cần load toàn bộ User entity.
     *
     * @param role     tên role dạng String (vd "TEACHER", "STUDENT") — so sánh bằng
     *                 role.equals("TEACHER"), không ép kiểu sang Role enum.
     * @param schoolId NULL nếu user là SUPER_ADMIN (không thuộc trường nào).
     * @param active   true nếu status=ACTIVE và chưa bị soft-delete (deletedAt=NULL).
     */
    record UserSummary(UUID id, String role, UUID schoolId, boolean active) {
    }

    /**
     * Trả về thông tin tóm tắt của user, hoặc Optional.empty() nếu userId không tồn tại.
     *
     * Dùng cho: SCHOOL_ADMIN tạo lớp hộ giáo viên (validate ownerTeacherId là TEACHER,
     * cùng trường, đang active), và thêm thành viên vào lớp (validate user tồn tại,
     * cùng trường với lớp, trước khi insert class_members).
     */
    Optional<UserSummary> findUserSummary(UUID userId);
}
