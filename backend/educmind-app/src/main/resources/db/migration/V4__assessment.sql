-- ============================================================================
-- V4 — NHÓM 3b: CÂU HỎI VÀ BÀI KIỂM TRA  (Spec v2, mục E — bảng 12..17)
-- ============================================================================

CREATE TYPE question_type     AS ENUM ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'ESSAY');
CREATE TYPE difficulty_level  AS ENUM ('EASY', 'MEDIUM', 'HARD');
CREATE TYPE question_source   AS ENUM ('MANUAL', 'AI_GENERATED');
CREATE TYPE attempt_status    AS ENUM ('IN_PROGRESS', 'SUBMITTED', 'GRADED');

-- Bảng 12: questions
CREATE TABLE questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID NOT NULL REFERENCES courses(id),
    content         TEXT NOT NULL,
    type            question_type NOT NULL,
    difficulty      difficulty_level NOT NULL,
    bloom_level     VARCHAR(30),
    options         JSONB,
    correct_answer  JSONB,
    source          question_source NOT NULL DEFAULT 'MANUAL',
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID REFERENCES users(id),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_choice_question_has_answer CHECK (
        type NOT IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')
        OR (options IS NOT NULL AND correct_answer IS NOT NULL)
    )
);

CREATE INDEX idx_questions_course ON questions(course_id);

-- Bảng 13: question_objectives (một câu hỏi có thể gắn nhiều mục tiêu hoặc không gắn)
CREATE TABLE question_objectives (
    question_id    UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    objective_id   UUID NOT NULL REFERENCES learning_objectives(id),
    CONSTRAINT pk_question_objectives PRIMARY KEY (question_id, objective_id)
);

CREATE INDEX idx_question_objectives_objective ON question_objectives(objective_id);

-- Bảng 14: assessments
CREATE TABLE assessments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id          UUID NOT NULL REFERENCES courses(id),
    topic_id           UUID REFERENCES course_topics(id),
    lesson_id          UUID REFERENCES lessons(id),
    title              VARCHAR(200) NOT NULL,
    duration_minutes   INT NOT NULL CHECK (duration_minutes > 0),
    max_attempts       INT NOT NULL CHECK (max_attempts >= 1),
    open_at            TIMESTAMPTZ NOT NULL,
    close_at           TIMESTAMPTZ NOT NULL,
    status             content_status NOT NULL DEFAULT 'DRAFT',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by         UUID REFERENCES users(id),
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT chk_close_after_open CHECK (close_at > open_at),
    CONSTRAINT chk_assessment_scope CHECK (num_nonnulls(topic_id, lesson_id) <= 1)
);

CREATE INDEX idx_assessments_course ON assessments(course_id);
CREATE INDEX idx_assessments_topic ON assessments(topic_id);
CREATE INDEX idx_assessments_lesson ON assessments(lesson_id);

-- Bảng 15: assessment_questions
CREATE TABLE assessment_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id   UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id),
    weight          NUMERIC(5,2) NOT NULL CHECK (weight > 0),
    CONSTRAINT uq_assessment_question UNIQUE (assessment_id, question_id)
);

CREATE INDEX idx_assessment_questions_question ON assessment_questions(question_id);

-- Bảng 16: assessment_attempts
CREATE TABLE assessment_attempts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id  UUID NOT NULL REFERENCES assessments(id),
    student_id     UUID NOT NULL REFERENCES users(id),
    attempt_number INT NOT NULL DEFAULT 1 CHECK (attempt_number >= 1),
    status         attempt_status NOT NULL DEFAULT 'IN_PROGRESS',
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at   TIMESTAMPTZ,
    graded_at      TIMESTAMPTZ,
    total_score    NUMERIC(6,2),
    CONSTRAINT uq_assessment_student_attempt UNIQUE (assessment_id, student_id, attempt_number)
);

CREATE INDEX idx_attempts_student ON assessment_attempts(student_id);
-- Mỗi học sinh chỉ có tối đa 1 lượt đang làm cho mỗi bài kiểm tra (AC-S03.2)
CREATE UNIQUE INDEX uq_one_in_progress_attempt
    ON assessment_attempts (assessment_id, student_id)
    WHERE status = 'IN_PROGRESS';

-- Bảng 17: submission_answers
CREATE TABLE submission_answers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id      UUID NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id),
    answer_content  JSONB,
    score           NUMERIC(5,2),
    feedback        TEXT,
    graded_by       UUID REFERENCES users(id),
    graded_at       TIMESTAMPTZ,
    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_answers_question ON submission_answers(question_id);
CREATE INDEX idx_answers_attempt ON submission_answers(attempt_id);
