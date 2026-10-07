package com.educmind.common.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tra cứu đối tượng nhận system announcement mà không để module-notification
 * phụ thuộc trực tiếp vào module-auth.
 */
public interface UserAnnouncementPort {

    /** Trường của một tài khoản; SUPER_ADMIN có thể không thuộc trường nào. */
    Optional<UUID> findSchoolIdByUserId(UUID userId);

    /** Các tài khoản ACTIVE, chưa bị xóa của một trường. */
    List<UUID> findActiveUserIdsBySchoolId(UUID schoolId);

    /** Các tài khoản ACTIVE, chưa bị xóa trong toàn hệ thống. */
    List<UUID> findAllActiveUserIds();

    /** Lọc danh sách userId, chỉ giữ tài khoản ACTIVE chưa bị xóa. */
    List<UUID> filterActiveUserIds(Collection<UUID> userIds);
}
