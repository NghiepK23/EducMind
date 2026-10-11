CREATE TABLE report_export_requests (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type       VARCHAR(30) NOT NULL,
    requested_by      UUID NOT NULL REFERENCES users(id),
    scope_params      JSONB NOT NULL,
    format            VARCHAR(10) NOT NULL DEFAULT 'CSV',
    status            VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    file_storage_key  VARCHAR(500),
    error_message     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,
    CONSTRAINT report_export_requests_type_check
        CHECK (report_type IN ('CLASS_PROGRESS', 'STUDENT_PROGRESS', 'ASSESSMENT_RESULT', 'AI_USAGE')),
    CONSTRAINT report_export_requests_format_check CHECK (format IN ('CSV', 'PDF')),
    CONSTRAINT report_export_requests_status_check
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_report_export_requests_requested_by
    ON report_export_requests(requested_by, created_at DESC);
