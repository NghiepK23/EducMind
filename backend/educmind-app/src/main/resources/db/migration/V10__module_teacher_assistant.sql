CREATE TABLE ai_generation_requests (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type     VARCHAR(30) NOT NULL
        CHECK (request_type IN ('LESSON_PLAN', 'QUESTION_SET', 'ASSESSMENT_MATRIX')),
    course_id        UUID NOT NULL REFERENCES courses(id),
    lesson_id        UUID NULL REFERENCES lessons(id),
    requested_by     UUID NOT NULL REFERENCES users(id),
    prompt_template  VARCHAR(50) NOT NULL,
    input_params     JSONB NULL,
    ai_provider      VARCHAR(30) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    error_message    TEXT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ NULL
);

CREATE INDEX idx_ai_gen_requests_requested_by ON ai_generation_requests(requested_by, created_at DESC);
CREATE INDEX idx_ai_gen_requests_course ON ai_generation_requests(course_id, created_at DESC);

CREATE TABLE ai_generation_outputs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id           UUID NOT NULL REFERENCES ai_generation_requests(id),
    content              JSONB NOT NULL,
    retrieved_chunk_ids  UUID[] NULL,
    review_status        VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'
        CHECK (review_status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EDITED_AND_APPROVED')),
    reviewed_by          UUID NULL REFERENCES users(id),
    reviewed_at          TIMESTAMPTZ NULL,
    review_notes         TEXT NULL,
    edited_content       JSONB NULL,
    applied_at           TIMESTAMPTZ NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_gen_outputs_request ON ai_generation_outputs(request_id);

CREATE TABLE ai_observability_logs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_context_type  VARCHAR(30) NOT NULL
        CHECK (request_context_type IN ('TEACHER_ASSISTANT', 'TUTOR', 'PERSONALIZATION')),
    request_context_id    UUID NOT NULL,
    actor_id              UUID NOT NULL REFERENCES users(id),
    provider              VARCHAR(30) NOT NULL,
    model                 VARCHAR(50) NOT NULL,
    task_type             VARCHAR(30) NOT NULL,
    latency_ms            INT NOT NULL,
    input_tokens          INT NULL,
    output_tokens         INT NULL,
    estimated_cost        NUMERIC(10,6) NULL,
    status                VARCHAR(20) NOT NULL
        CHECK (status IN ('SUCCESS', 'FAILED')),
    error_message         TEXT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_obs_actor_created ON ai_observability_logs(actor_id, created_at DESC);
CREATE INDEX idx_ai_obs_context ON ai_observability_logs(request_context_type, request_context_id);
