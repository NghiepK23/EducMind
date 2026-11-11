-- ============================================================================
-- V6 — NHÓM 5 + 6: AI HỖ TRỢ GIÁO VIÊN & THÔNG BÁO  (Spec v2, mục G, H)
-- ============================================================================

CREATE TYPE ai_request_status AS ENUM ('PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE ai_review_status  AS ENUM ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EDITED_AND_APPROVED');
CREATE TYPE notification_type AS ENUM (
    'NEW_SUBMISSION',
    'SUBMISSION_RESUBMITTED',
    'NEW_ASSESSMENT_ATTEMPT_SUBMITTED',
    'AI_GENERATION_COMPLETED',
    'AI_GENERATION_FAILED',
    'NEW_LESSON_PUBLISHED',
    'NEW_ASSIGNMENT_PUBLISHED',
    'NEW_ASSESSMENT_PUBLISHED',
    'ASSIGNMENT_GRADED',
    'ASSESSMENT_GRADED',
    'NEW_RECOMMENDATION'
);

-- Bảng 22: ai_generation_requests (chỉ sinh câu hỏi nháp cho 1 bài học)
CREATE TABLE ai_generation_requests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id     UUID NOT NULL REFERENCES users(id),
    lesson_id      UUID NOT NULL REFERENCES lessons(id),
    quantity       INT NOT NULL CHECK (quantity > 0),
    params         JSONB,
    status         ai_request_status NOT NULL DEFAULT 'PROCESSING',
    error_message  TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_ai_requests_teacher ON ai_generation_requests(teacher_id, created_at);

-- Bảng 23: ai_generation_outputs
CREATE TABLE ai_generation_outputs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id           UUID NOT NULL REFERENCES ai_generation_requests(id) ON DELETE CASCADE,
    content              TEXT NOT NULL,
    type                 question_type NOT NULL,
    objective_id         UUID NOT NULL REFERENCES learning_objectives(id),
    difficulty           difficulty_level NOT NULL DEFAULT 'MEDIUM',
    bloom_level          VARCHAR(30),
    options              JSONB,
    correct_answer       JSONB,
    review_status        ai_review_status NOT NULL DEFAULT 'PENDING_REVIEW',
    review_note          TEXT,
    reviewed_by          UUID REFERENCES users(id),
    reviewed_at          TIMESTAMPTZ,
    approved_question_id UUID REFERENCES questions(id),
    CONSTRAINT chk_ai_review_consistency CHECK (
        (review_status = 'PENDING_REVIEW'
            AND reviewed_by IS NULL AND reviewed_at IS NULL AND approved_question_id IS NULL)
        OR (review_status = 'REJECTED'
            AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND approved_question_id IS NULL)
        OR (review_status IN ('APPROVED', 'EDITED_AND_APPROVED')
            AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND approved_question_id IS NOT NULL)
    )
);

CREATE INDEX idx_ai_outputs_request ON ai_generation_outputs(request_id);
CREATE INDEX idx_ai_outputs_review_status ON ai_generation_outputs(review_status);

-- Bảng 25: notifications (được sinh tự động bởi trigger — xem V7, mục J.4)
CREATE TABLE notifications (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id),
    type                 notification_type NOT NULL,
    title                VARCHAR(255) NOT NULL,
    content              TEXT,
    related_entity_type  VARCHAR(50),
    related_entity_id    UUID,
    is_read              BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at              TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, is_read, created_at DESC);
