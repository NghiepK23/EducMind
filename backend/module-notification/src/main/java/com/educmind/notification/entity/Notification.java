package com.educmind.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Bảng notifications — V8__module_notification.sql, Module 10 mục J.4.1. */
@Entity
@Table(name = "notifications")
public class Notification {

    public static final String STATUS_UNREAD = "UNREAD";
    public static final String STATUS_READ = "READ";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "source_event_type", nullable = false, length = 40)
    private String sourceEventType;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Column(nullable = false, length = 10)
    private String status = STATUS_UNREAD;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    /** Do DB sinh (BIGSERIAL), chỉ đọc — dùng cho cursor polling since_seq. */
    @Column(name = "sequence_number", insertable = false, updatable = false)
    private Long sequenceNumber;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Notification() {
    }

    public Notification(UUID recipientId, String type, String title, String body,
                        Map<String, Object> payload, String sourceEventType, UUID sourceEntityId) {
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.payload = payload;
        this.sourceEventType = sourceEventType;
        this.sourceEntityId = sourceEntityId;
    }

    public void markRead() {
        if (STATUS_UNREAD.equals(status)) {
            this.status = STATUS_READ;
            this.readAt = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, Object> getPayload() { return payload; }
    public String getSourceEventType() { return sourceEventType; }
    public UUID getSourceEntityId() { return sourceEntityId; }
    public String getStatus() { return status; }
    public OffsetDateTime getReadAt() { return readAt; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
