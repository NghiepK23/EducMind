package com.educmind.common.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port tra cứu thông tin cơ bản của user mà không phụ thuộc module-auth.
 * role dùng String ("TEACHER"/"STUDENT") để educmind-common giữ trung lập.
 */
public interface UserLookupPort {

    record UserSummary(UUID id, String role, boolean active) {
    }

    /** Thông tin hiển thị (danh sách học sinh của lớp). */
    record UserDetail(UUID id, String fullName, String email, String role) {
    }

    /** empty nếu user không tồn tại hoặc đã bị xóa mềm. */
    Optional<UserSummary> findUserSummary(UUID userId);

    /** Trả về các user tồn tại (bỏ qua id không tồn tại), giữ thứ tự đầu vào. */
    List<UserDetail> findUserDetails(Collection<UUID> userIds);
}
