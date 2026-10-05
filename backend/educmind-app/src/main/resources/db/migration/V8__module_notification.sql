-- Module 10 (Phan J): Notification
-- sequence_number (BIGSERIAL) dung rieng cho cursor polling (since_seq),
-- id (UUID) dung cho tham chieu. source_entity_id la polymorphic nen khong co FK cung.
-- Cot type khong co CHECK de them loai notification moi khong can migration.

CREATE TABLE notifications (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id       UUID NOT NULL REFERENCES users(id),
    type               VARCHAR(40) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    body               VARCHAR(1000) NOT NULL,
    payload            JSONB NULL,
    source_event_type  VARCHAR(40) NOT NULL,
    source_entity_id   UUID NULL,
    status             VARCHAR(10) NOT NULL DEFAULT 'UNREAD',
    read_at            TIMESTAMPTZ NULL,
    sequence_number    BIGSERIAL NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notifications_status CHECK (status IN ('UNREAD', 'READ'))
);

CREATE INDEX idx_notifications_recipient_status
    ON notifications (recipient_id, status, created_at DESC);
CREATE INDEX idx_notifications_recipient_sequence
    ON notifications (recipient_id, sequence_number DESC);

CREATE TABLE notification_preferences (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id),
    notification_type  VARCHAR(40) NOT NULL,
    enabled            BOOLEAN NOT NULL DEFAULT true,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_preferences UNIQUE (user_id, notification_type)
);
