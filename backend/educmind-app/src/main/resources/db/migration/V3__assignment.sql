-- ============================================================================
-- V3 — NHÓM 3a: BÀI TẬP  (Spec v2, mục E — bảng 9..11)
-- ============================================================================

CREATE TYPE submission_status AS ENUM ('SUBMITTED', 'LATE');

-- Bảng 9: assignments
CREATE TABLE assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID NOT NULL REFERENCES courses(id),
    topic_id        UUID REFERENCES course_topics(id),
    lesson_id       UUID REFERENCES lessons(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    due_date        TIMESTAMPTZ NOT NULL,
    max_score       NUMERIC(5,2) NOT NULL CHECK (max_score > 0),
    allow_late      BOOLEAN NOT NULL DEFAULT false,
    late_close_date TIMESTAMPTZ,
    status          content_status NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID REFERENCES users(id),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_late_close CHECK (
        (allow_late = false AND late_close_date IS NULL)
        OR (allow_late = true AND late_close_date > due_date)
    ),
    -- Phạm vi: Khóa học (cả hai NULL) / Chủ đề (chỉ topic_id) / Bài học (chỉ lesson_id)
    CONSTRAINT chk_assignment_scope CHECK (num_nonnulls(topic_id, lesson_id) <= 1)
);

CREATE INDEX idx_assignments_course ON assignments(course_id);
CREATE INDEX idx_assignments_topic ON assignments(topic_id);
CREATE INDEX idx_assignments_lesson ON assignments(lesson_id);

-- Bảng 10: assignment_submissions
CREATE TABLE assignment_submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID NOT NULL REFERENCES assignments(id),
    student_id      UUID NOT NULL REFERENCES users(id),
    text_content    TEXT,
    file_url        VARCHAR(500),
    status          submission_status NOT NULL,
    score           NUMERIC(5,2) CHECK (score >= 0),
    feedback        TEXT,
    graded_by       UUID REFERENCES users(id),
    graded_at       TIMESTAMPTZ,
    is_edited       BOOLEAN NOT NULL DEFAULT false,
    edited_at       TIMESTAMPTZ,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_assignment_student UNIQUE (assignment_id, student_id),
    CONSTRAINT chk_submission_has_content CHECK (text_content IS NOT NULL OR file_url IS NOT NULL)
);

CREATE INDEX idx_submissions_student ON assignment_submissions(student_id);

-- Bảng 11: assignment_objectives (chỉ mục tiêu được liên kết mới nhận bằng chứng)
CREATE TABLE assignment_objectives (
    assignment_id  UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    objective_id   UUID NOT NULL REFERENCES learning_objectives(id),
    CONSTRAINT pk_assignment_objectives PRIMARY KEY (assignment_id, objective_id)
);

CREATE INDEX idx_assignment_objectives_objective ON assignment_objectives(objective_id);
