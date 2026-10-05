package com.educmind.notification.repository;

import com.educmind.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, String notificationType);

    List<NotificationPreference> findByUserId(UUID userId);

    /** Trong danh sách userIds, những ai đã TẮT loại notification này. Caller đảm bảo userIds không rỗng. */
    @Query("SELECT p.userId FROM NotificationPreference p "
            + "WHERE p.notificationType = :type AND p.enabled = false AND p.userId IN :userIds")
    List<UUID> findDisabledUserIds(@Param("type") String type, @Param("userIds") Collection<UUID> userIds);
}
