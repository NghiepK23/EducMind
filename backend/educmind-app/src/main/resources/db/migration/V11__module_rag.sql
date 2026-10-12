CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id      UUID NOT NULL REFERENCES resources(id),
    course_id        UUID NOT NULL REFERENCES courses(id),
    lesson_id        UUID NULL REFERENCES lessons(id),
    chunk_index      INT NOT NULL,
    content          TEXT NOT NULL,
    embedding        VECTOR(768) NOT NULL,
    embedding_model  VARCHAR(100) NOT NULL,
    metadata         JSONB NULL,
    content_tsv      TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_chunks_resource_index UNIQUE (resource_id, chunk_index)
);

CREATE INDEX idx_document_chunks_course_id ON document_chunks(course_id);
CREATE INDEX idx_document_chunks_lesson_id ON document_chunks(lesson_id);
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_document_chunks_content_tsv ON document_chunks USING gin (content_tsv);

CREATE TABLE resource_processing_jobs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id    UUID NOT NULL REFERENCES resources(id),
    stage          VARCHAR(30) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count  INT NOT NULL DEFAULT 0,
    error_message  TEXT NULL,
    started_at     TIMESTAMPTZ NULL,
    finished_at    TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT resource_processing_jobs_status_check
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_resource_processing_jobs_resource_id ON resource_processing_jobs(resource_id);
