package com.educmind.notification.repository;

import com.educmind.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** statuses: tập trạng thái cần lấy (UNREAD, READ hoặc cả hai). sinceSeq = null -> mới nhất trước. */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :uid AND n.status IN :statuses "
            + "ORDER BY n.sequenceNumber DESC")
    List<Notification> findLatest(@Param("uid") UUID uid, @Param("statuses") List<String> statuses,
                                  Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :uid AND n.status IN :statuses "
            + "AND n.sequenceNumber > :sinceSeq ORDER BY n.sequenceNumber ASC")
    List<Notification> findSince(@Param("uid") UUID uid, @Param("statuses") List<String> statuses,
                                 @Param("sinceSeq") long sinceSeq, Pageable pageable);

    @Query("SELECT COALESCE(MAX(n.sequenceNumber), 0) FROM Notification n WHERE n.recipientId = :uid")
    long findLatestSeq(@Param("uid") UUID uid);

    long countByRecipientIdAndStatus(UUID recipientId, String status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = CURRENT_TIMESTAMP "
            + "WHERE n.recipientId = :uid AND n.status = 'UNREAD'")
    int markAllRead(@Param("uid") UUID uid);
}
