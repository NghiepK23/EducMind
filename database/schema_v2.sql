-- ============================================================================
-- EDUCMIND — SCHEMA V2 (theo "Báo cáo" — mục III. Cơ sở dữ liệu)
-- ============================================================================
-- Yêu cầu: PostgreSQL 11 trở lên. Từ bản 13, gen_random_uuid() có sẵn;
-- ở bản 11–12 cần extension pgcrypto (dòng CREATE EXTENSION bên dưới).
-- Script dành cho CSDL trống: chạy MỘT LẦN, theo đúng thứ tự A → J.
--
-- KHÁC BIỆT so với schema đang chạy trong backend (V1..V11):
--   * 2 vai trò duy nhất: TEACHER / STUDENT (bỏ SUPER_ADMIN, SCHOOL_ADMIN)
--   * Không còn schools / invite_codes / refresh_tokens / course_class
--   * Thêm course_topics (Khóa học → Chủ đề → Bài học)
--   * learning_objectives là BẢNG RIÊNG (bỏ JSONB trên lessons)
--   * Thêm bảng nối assignment_objectives, question_objectives
--   * Bỏ: document_chunks (RAG), subjects, report exports, notification_preferences,
--          ai_observability_logs, tutor/personalization tables
-- ============================================================================


-- ============================================================================
-- A. EXTENSION
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ============================================================================
-- B. ENUM TYPES
-- ============================================================================
CREATE TYPE user_role             AS ENUM ('TEACHER', 'STUDENT');
CREATE TYPE content_status        AS ENUM ('DRAFT', 'PUBLISHED');
CREATE TYPE submission_status     AS ENUM ('SUBMITTED', 'LATE');
CREATE TYPE question_type         AS ENUM ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'ESSAY');
CREATE TYPE difficulty_level      AS ENUM ('EASY', 'MEDIUM', 'HARD');
CREATE TYPE question_source       AS ENUM ('MANUAL', 'AI_GENERATED');
CREATE TYPE attempt_status        AS ENUM ('IN_PROGRESS', 'SUBMITTED', 'GRADED');
CREATE TYPE evidence_source       AS ENUM ('ASSIGNMENT', 'ASSESSMENT');
CREATE TYPE gap_status            AS ENUM ('OPEN', 'RESOLVED', 'DISMISSED');
CREATE TYPE recommendation_type   AS ENUM ('REVIEW_CONTENT', 'PRACTICE_TARGETED', 'ADVANCED_ACTIVITY', 'REATTEMPT_ASSESSMENT');
CREATE TYPE recommendation_status AS ENUM ('ACTIVE', 'ACCEPTED', 'DISMISSED', 'EXPIRED');
CREATE TYPE ai_request_status     AS ENUM ('PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE ai_review_status      AS ENUM ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EDITED_AND_APPROVED');
CREATE TYPE notification_type     AS ENUM (
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


-- ============================================================================
-- C. NHÓM 1 — NGƯỜI DÙNG VÀ LỚP HỌC
-- ============================================================================

-- Bảng 1: users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL,   -- duy nhất theo lower(email), xem uq_users_email_lower (phần I)
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

-- Bảng 2: classes
CREATE TABLE classes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    grade               SMALLINT,
    school_year         VARCHAR(20),
    owner_teacher_id    UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID REFERENCES users(id),
    deleted_at          TIMESTAMPTZ
);

-- Bảng 3: class_members
CREATE TABLE class_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id    UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id  UUID NOT NULL REFERENCES users(id),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_class_student UNIQUE (class_id, student_id)
);


-- ============================================================================
-- D. NHÓM 2 — NỘI DUNG DẠY HỌC
-- ============================================================================

-- Bảng 4: courses
CREATE TABLE courses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id     UUID NOT NULL REFERENCES classes(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID REFERENCES users(id),
    deleted_at   TIMESTAMPTZ
);

-- Bảng 5: course_topics
CREATE TABLE course_topics (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id      UUID NOT NULL REFERENCES courses(id),
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    display_order  INT NOT NULL DEFAULT 0,
    status         content_status NOT NULL DEFAULT 'DRAFT',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by     UUID REFERENCES users(id),
    deleted_at     TIMESTAMPTZ
);

-- Bảng 6: lessons
CREATE TABLE lessons (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID NOT NULL REFERENCES courses(id),
    topic_id     UUID NOT NULL REFERENCES course_topics(id),  -- chủ đề phải cùng course_id, xem fn_check_lesson_topic_course (J.5)
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    status       content_status NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID REFERENCES users(id),
    deleted_at   TIMESTAMPTZ
);

-- Bảng 7: learning_objectives
CREATE TABLE learning_objectives (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id    UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    code         VARCHAR(20) NOT NULL,
    description  TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_objective_code UNIQUE (lesson_id, code)
);

-- Bảng 8: resources
CREATE TABLE resources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID REFERENCES courses(id),
    lesson_id       UUID REFERENCES lessons(id),
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0 AND file_size_bytes <= 52428800),  -- 50 MB
    file_type       VARCHAR(50) NOT NULL,
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_resource_owner CHECK (course_id IS NOT NULL OR lesson_id IS NOT NULL)
);


-- ============================================================================
-- E. NHÓM 3 — BÀI TẬP, CÂU HỎI, BÀI KIỂM TRA
-- ============================================================================

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
    -- Phạm vi: Khóa học (topic_id, lesson_id đều NULL) / Chủ đề (chỉ topic_id) / Bài học (chỉ lesson_id)
    CONSTRAINT chk_assignment_scope CHECK (num_nonnulls(topic_id, lesson_id) <= 1)
);

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

-- Bảng 11: assignment_objectives
CREATE TABLE assignment_objectives (
    assignment_id  UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    objective_id   UUID NOT NULL REFERENCES learning_objectives(id),
    CONSTRAINT pk_assignment_objectives PRIMARY KEY (assignment_id, objective_id)
);

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

-- Bảng 13: question_objectives
CREATE TABLE question_objectives (
    question_id    UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    objective_id   UUID NOT NULL REFERENCES learning_objectives(id),
    CONSTRAINT pk_question_objectives PRIMARY KEY (question_id, objective_id)
);

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
    -- Phạm vi: Khóa học / Chủ đề / Bài học; chỉ được thuộc một cấp
    CONSTRAINT chk_assessment_scope CHECK (num_nonnulls(topic_id, lesson_id) <= 1)
);

-- Bảng 15: assessment_questions
CREATE TABLE assessment_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id   UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id),
    weight          NUMERIC(5,2) NOT NULL CHECK (weight > 0),
    CONSTRAINT uq_assessment_question UNIQUE (assessment_id, question_id)
);

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


-- ============================================================================
-- F. NHÓM 4 — BẰNG CHỨNG HỌC TẬP VÀ CÁ NHÂN HÓA
-- ============================================================================

-- Bảng 18: learning_evidence
CREATE TABLE learning_evidence (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID NOT NULL REFERENCES users(id),
    objective_id  UUID NOT NULL REFERENCES learning_objectives(id),
    source_type   evidence_source NOT NULL,
    -- source_id: ASSIGNMENT -> id bài nộp (assignment_submissions);
    --            ASSESSMENT -> id bài kiểm tra (assessments); điểm lấy từ lượt làm có total_score cao nhất.
    -- Chỉ mục tiêu thực sự liên kết (assignment_objectives / question_objectives) mới có bằng chứng.
    -- Bài kiểm tra: score = SUM(điểm đạt phân bổ) / SUM(điểm tối đa phân bổ); câu hỏi liên kết n
    --            mục tiêu thì chia đều cả điểm đạt và điểm tối đa của câu cho n mục tiêu (US-S03).
    -- Tính hợp lệ của source_id được kiểm tra bằng trigger fn_check_evidence_source (phần J.5).
    source_id     UUID NOT NULL,
    score         NUMERIC(4,3) NOT NULL CHECK (score >= 0 AND score <= 1),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),   -- mốc tính tuổi bằng chứng (hệ số suy giảm)
    CONSTRAINT uq_evidence_source UNIQUE (student_id, objective_id, source_type, source_id)
);

-- Bảng 19: mastery
CREATE TABLE mastery (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL REFERENCES users(id),
    objective_id    UUID NOT NULL REFERENCES learning_objectives(id),
    mastery_score   NUMERIC(4,3) NOT NULL CHECK (mastery_score >= 0 AND mastery_score <= 1),
    evidence_count  INT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_objective_mastery UNIQUE (student_id, objective_id)
);

-- Bảng 20: knowledge_gaps
CREATE TABLE knowledge_gaps (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES users(id),
    objective_id   UUID NOT NULL REFERENCES learning_objectives(id),
    status         gap_status NOT NULL DEFAULT 'OPEN',
    reason         TEXT,
    opened_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at    TIMESTAMPTZ,
    dismissed_by   UUID REFERENCES users(id),
    dismissed_at   TIMESTAMPTZ,
    dismiss_reason TEXT
);

-- Bảng 21: recommendations
CREATE TABLE recommendations (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id             UUID NOT NULL REFERENCES users(id),
    objective_id           UUID REFERENCES learning_objectives(id),
    related_assessment_id  UUID REFERENCES assessments(id),
    type                   recommendation_type NOT NULL,
    reason                 TEXT,
    target_entity_type     VARCHAR(50),
    target_entity_id       UUID,
    status                 recommendation_status NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at           TIMESTAMPTZ
);


-- ============================================================================
-- G. NHÓM 5 — AI HỖ TRỢ GIÁO VIÊN
-- ============================================================================

-- Bảng 22: ai_generation_requests
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


-- ============================================================================
-- H. NHÓM 6 — TÀI KHOẢN VÀ THÔNG BÁO
-- ============================================================================

-- Bảng 24: password_reset_tokens
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Bảng 25: notifications
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


-- ============================================================================
-- I. INDEX
-- ============================================================================
-- Nhóm 1
CREATE INDEX idx_classes_owner              ON classes(owner_teacher_id);
CREATE INDEX idx_class_members_student      ON class_members(student_id);
CREATE UNIQUE INDEX uq_users_email_lower    ON users (lower(email)) WHERE deleted_at IS NULL;

-- Nhóm 2
CREATE INDEX idx_courses_class              ON courses(class_id);
CREATE INDEX idx_topics_course              ON course_topics(course_id);
CREATE INDEX idx_lessons_course             ON lessons(course_id);
CREATE INDEX idx_lessons_topic              ON lessons(topic_id);
CREATE INDEX idx_lo_lesson                  ON learning_objectives(lesson_id);
CREATE INDEX idx_resources_lesson           ON resources(lesson_id);
CREATE INDEX idx_resources_course           ON resources(course_id);

-- Nhóm 3
CREATE INDEX idx_assignments_course         ON assignments(course_id);
CREATE INDEX idx_assignments_topic          ON assignments(topic_id);
CREATE INDEX idx_assignments_lesson         ON assignments(lesson_id);
CREATE INDEX idx_assignment_objectives_objective ON assignment_objectives(objective_id);
CREATE INDEX idx_submissions_student        ON assignment_submissions(student_id);
CREATE INDEX idx_questions_course           ON questions(course_id);
CREATE INDEX idx_question_objectives_objective ON question_objectives(objective_id);
CREATE INDEX idx_assessments_course         ON assessments(course_id);
CREATE INDEX idx_assessments_topic          ON assessments(topic_id);
CREATE INDEX idx_assessments_lesson         ON assessments(lesson_id);
CREATE INDEX idx_assessment_questions_question ON assessment_questions(question_id);
CREATE INDEX idx_attempts_student           ON assessment_attempts(student_id);
CREATE INDEX idx_answers_question           ON submission_answers(question_id);
CREATE INDEX idx_answers_attempt            ON submission_answers(attempt_id);
CREATE UNIQUE INDEX uq_one_in_progress_attempt
    ON assessment_attempts (assessment_id, student_id)
    WHERE status = 'IN_PROGRESS';

-- Nhóm 4
CREATE INDEX idx_evidence_student_objective ON learning_evidence(student_id, objective_id);
CREATE INDEX idx_evidence_source            ON learning_evidence(source_type, source_id);
CREATE INDEX idx_evidence_objective         ON learning_evidence(objective_id);
CREATE INDEX idx_mastery_objective          ON mastery(objective_id);
CREATE INDEX idx_gaps_objective             ON knowledge_gaps(objective_id);
CREATE INDEX idx_recommend_objective        ON recommendations(objective_id);
CREATE INDEX idx_gaps_student_status        ON knowledge_gaps(student_id, status);
CREATE INDEX idx_recommend_student_status   ON recommendations(student_id, status);

-- Ràng buộc duy nhất có điều kiện (partial unique index)
CREATE UNIQUE INDEX uq_open_gap_per_objective
    ON knowledge_gaps (student_id, objective_id)
    WHERE status = 'OPEN';

-- COALESCE để NULL (gợi ý làm lại bài, không có mục tiêu) vẫn bị coi là trùng;
-- related_assessment_id giúp hai bài kiểm tra khác nhau cùng mục tiêu không chặn nhau.
CREATE UNIQUE INDEX uq_active_recommendation
    ON recommendations (
        student_id,
        type,
        COALESCE(objective_id, '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(related_assessment_id, '00000000-0000-0000-0000-000000000000'::uuid)
    )
    WHERE status = 'ACTIVE';

-- Nhóm 5
CREATE INDEX idx_ai_requests_teacher        ON ai_generation_requests(teacher_id, created_at);
CREATE INDEX idx_ai_outputs_request         ON ai_generation_outputs(request_id);
CREATE INDEX idx_ai_outputs_review_status   ON ai_generation_outputs(review_status);

-- Nhóm 6
CREATE UNIQUE INDEX uq_reset_token_hash     ON password_reset_tokens(token_hash);

CREATE UNIQUE INDEX uq_active_reset_token
    ON password_reset_tokens (user_id)
    WHERE used_at IS NULL;

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, is_read, created_at DESC);


-- ============================================================================
-- J. TRIGGER FUNCTIONS & TRIGGERS
-- ============================================================================

-- ----------------------------------------------------------------------------
-- J.1. Cập nhật updated_at tự động
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_assignments_updated_at
    BEFORE UPDATE ON assignments
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_classes_updated_at
    BEFORE UPDATE ON classes
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_courses_updated_at
    BEFORE UPDATE ON courses
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_course_topics_updated_at
    BEFORE UPDATE ON course_topics
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_lessons_updated_at
    BEFORE UPDATE ON lessons
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_questions_updated_at
    BEFORE UPDATE ON questions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_assessments_updated_at
    BEFORE UPDATE ON assessments
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_learning_evidence_updated_at
    BEFORE UPDATE ON learning_evidence
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ----------------------------------------------------------------------------
-- J.2. Ràng buộc nghiệp vụ — Chấm điểm & Bài kiểm tra
-- ----------------------------------------------------------------------------

-- US-T04 Task 2: điểm chấm phải nằm trong [0, max_score]
CREATE OR REPLACE FUNCTION fn_check_submission_score()
RETURNS TRIGGER AS $$
DECLARE
    v_max_score NUMERIC(5,2);
BEGIN
    IF NEW.score IS NOT NULL THEN
        SELECT max_score INTO v_max_score
        FROM assignments WHERE id = NEW.assignment_id;

        IF NEW.score < 0 OR NEW.score > v_max_score THEN
            RAISE EXCEPTION 'Điểm % vượt khoảng cho phép [0, %]', NEW.score, v_max_score;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_submission_score
    BEFORE INSERT OR UPDATE ON assignment_submissions
    FOR EACH ROW EXECUTE FUNCTION fn_check_submission_score();

-- US-T06: không sửa câu hỏi của bài kiểm tra đã có người làm
CREATE OR REPLACE FUNCTION fn_prevent_edit_assessment_questions()
RETURNS TRIGGER AS $$
DECLARE
    v_assessment_id UUID;
BEGIN
    v_assessment_id := COALESCE(NEW.assessment_id, OLD.assessment_id);

    IF EXISTS (
        SELECT 1 FROM assessment_attempts
        WHERE assessment_id = v_assessment_id
    ) THEN
        RAISE EXCEPTION 'Không thể sửa câu hỏi: bài kiểm tra đã có học sinh làm bài';
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_edit_assessment_questions
    BEFORE INSERT OR UPDATE OR DELETE ON assessment_questions
    FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_assessment_questions();

-- US-T15: điểm từng câu phải nằm trong [0, trọng số của câu trong bài kiểm tra]
CREATE OR REPLACE FUNCTION fn_check_answer_score()
RETURNS TRIGGER AS $$
DECLARE
    v_weight NUMERIC(5,2);
BEGIN
    IF NEW.score IS NOT NULL THEN
        SELECT aq.weight INTO v_weight
        FROM assessment_attempts att
        JOIN assessment_questions aq
          ON aq.assessment_id = att.assessment_id
         AND aq.question_id = NEW.question_id
        WHERE att.id = NEW.attempt_id;

        IF v_weight IS NULL THEN
            RAISE EXCEPTION 'Câu hỏi không thuộc bài kiểm tra của lượt làm này';
        END IF;
        IF NEW.score < 0 OR NEW.score > v_weight THEN
            RAISE EXCEPTION 'Điểm % vượt khoảng cho phép [0, %]', NEW.score, v_weight;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_answer_score
    BEFORE INSERT OR UPDATE ON submission_answers
    FOR EACH ROW EXECUTE FUNCTION fn_check_answer_score();


-- ----------------------------------------------------------------------------
-- J.3. Ràng buộc nghiệp vụ — Chỉnh sửa/Xóa nội dung (US-T11, US-T12, US-S07)
-- ----------------------------------------------------------------------------

-- US-S02, US-S07, US-T04: sửa / xóa mềm / nộp lại bài nộp của học sinh
-- Thời hạn nộp: trước due_date, hoặc (allow_late = true và chưa quá late_close_date).
CREATE OR REPLACE FUNCTION fn_restrict_submission_edit()
RETURNS TRIGGER AS $$
DECLARE
    v_due_date        TIMESTAMPTZ;
    v_allow_late      BOOLEAN;
    v_late_close_date TIMESTAMPTZ;
    v_in_window       BOOLEAN;
BEGIN
    SELECT due_date, allow_late, late_close_date
      INTO v_due_date, v_allow_late, v_late_close_date
      FROM assignments
     WHERE id = NEW.assignment_id;

    v_in_window := now() <= v_due_date
                   OR (v_allow_late AND now() <= v_late_close_date);

    -- (A) Dòng đã xóa mềm: không cho sửa; chỉ được khôi phục qua nhánh (B)
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Bài nộp đã bị xóa, không thể chỉnh sửa (id=%)', OLD.id;
    END IF;

    -- (B) NỘP LẠI = khôi phục dòng đã xóa mềm (deleted_at: NOT NULL -> NULL)
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        IF NOT v_in_window THEN
            RAISE EXCEPTION 'Đã quá thời hạn nộp bài, không thể nộp lại (assignment_id=%)', NEW.assignment_id;
        END IF;
        -- Coi như một lượt nộp mới: đặt lại các trường do giáo viên quản lý
        NEW.submitted_at := now();
        NEW.status := CASE WHEN now() <= v_due_date
                           THEN 'SUBMITTED'::submission_status
                           ELSE 'LATE'::submission_status END;
        NEW.score        := NULL;
        NEW.feedback     := NULL;
        NEW.graded_by    := NULL;
        NEW.graded_at    := NULL;
        NEW.is_edited    := FALSE;
        NEW.edited_at    := NULL;
        RETURN NEW;
    END IF;

    -- (C) XÓA MỀM bài nộp: chỉ khi chưa chấm và còn trong thời hạn nộp
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
        IF OLD.graded_at IS NOT NULL THEN
            RAISE EXCEPTION 'Bài nộp đã được chấm, không thể xóa (id=%)', OLD.id;
        END IF;
        IF NOT v_in_window THEN
            RAISE EXCEPTION 'Đã quá thời hạn nộp bài, không thể xóa bài nộp (assignment_id=%)', NEW.assignment_id;
        END IF;
        RETURN NEW;
    END IF;

    -- (D) Cả hai chưa xóa
    -- (D1) Luồng chấm / chấm lại của giáo viên (điểm, nhận xét, thông tin chấm thay đổi)
    IF NEW.score        IS DISTINCT FROM OLD.score
       OR NEW.feedback  IS DISTINCT FROM OLD.feedback
       OR NEW.graded_by IS DISTINCT FROM OLD.graded_by
       OR NEW.graded_at IS DISTINCT FROM OLD.graded_at THEN
        IF NEW.text_content IS DISTINCT FROM OLD.text_content
           OR NEW.file_url  IS DISTINCT FROM OLD.file_url THEN
            RAISE EXCEPTION 'Không được đồng thời sửa nội dung bài làm và điểm/nhận xét trong một thao tác';
        END IF;
        RETURN NEW;   -- chấm bài không phụ thuộc thời hạn nộp
    END IF;
    -- Ghi chú: DB không biết vai trò người thao tác; việc chỉ giáo viên được chấm
    -- do tầng ứng dụng kiểm soát.

    -- (D2) Học sinh sửa nội dung bài làm
    IF NEW.text_content IS DISTINCT FROM OLD.text_content
       OR NEW.file_url  IS DISTINCT FROM OLD.file_url THEN
        IF OLD.graded_at IS NOT NULL THEN
            RAISE EXCEPTION 'Bài nộp đã được chấm, không thể chỉnh sửa (id=%)', OLD.id;
        END IF;
        IF NOT v_in_window THEN
            RAISE EXCEPTION 'Đã quá thời hạn nộp bài, không thể chỉnh sửa bài làm (assignment_id=%)', NEW.assignment_id;
        END IF;
        NEW.is_edited := TRUE;
        NEW.edited_at := now();
        -- Tính lại đúng hạn / nộp trễ theo thời điểm sửa
        NEW.status := CASE WHEN now() <= v_due_date
                           THEN 'SUBMITTED'::submission_status
                           ELSE 'LATE'::submission_status END;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restrict_submission_edit
    BEFORE UPDATE ON assignment_submissions
    FOR EACH ROW
    EXECUTE FUNCTION fn_restrict_submission_edit();

-- US-S07 Task 3: xóa evidence khi bài nộp bị xóa mềm
CREATE OR REPLACE FUNCTION fn_cleanup_evidence_on_submission_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
        DELETE FROM learning_evidence
        WHERE source_type = 'ASSIGNMENT' AND source_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cleanup_evidence_on_submission_delete
    AFTER UPDATE ON assignment_submissions
    FOR EACH ROW EXECUTE FUNCTION fn_cleanup_evidence_on_submission_delete();

-- US-T11: không cho sửa nội dung, đáp án, loại câu hỏi của câu hỏi đã có người làm
CREATE OR REPLACE FUNCTION fn_restrict_question_edit()
RETURNS TRIGGER AS $$
DECLARE
    v_used_count INT;
BEGIN
    IF NEW.content IS DISTINCT FROM OLD.content
       OR NEW.options IS DISTINCT FROM OLD.options
       OR NEW.correct_answer IS DISTINCT FROM OLD.correct_answer
       OR NEW.type IS DISTINCT FROM OLD.type THEN

        SELECT COUNT(*) INTO v_used_count
        FROM assessment_questions aq
        JOIN assessment_attempts att ON att.assessment_id = aq.assessment_id
        WHERE aq.question_id = OLD.id;

        IF v_used_count > 0 THEN
            RAISE EXCEPTION 'Không thể sửa nội dung/đáp án/loại câu hỏi: câu hỏi đã được dùng trong bài kiểm tra có học sinh làm';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restrict_question_edit
    BEFORE UPDATE ON questions
    FOR EACH ROW EXECUTE FUNCTION fn_restrict_question_edit();

-- US-T11: không cho thêm/bớt mục tiêu học tập của câu hỏi đã có người làm
CREATE OR REPLACE FUNCTION fn_restrict_question_objective_edit()
RETURNS TRIGGER AS $$
DECLARE
    v_question_id UUID;
BEGIN
    v_question_id := COALESCE(NEW.question_id, OLD.question_id);
    IF EXISTS (
        SELECT 1
          FROM assessment_questions aq
          JOIN assessment_attempts att ON att.assessment_id = aq.assessment_id
         WHERE aq.question_id = v_question_id
    ) THEN
        RAISE EXCEPTION 'Không thể sửa mục tiêu học tập: câu hỏi đã được dùng trong bài kiểm tra có học sinh làm';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restrict_question_objective_edit
    BEFORE INSERT OR UPDATE OR DELETE ON question_objectives
    FOR EACH ROW EXECUTE FUNCTION fn_restrict_question_objective_edit();

-- US-T11: khóa điểm tối đa khi bài tập đã có bài nộp được chấm
CREATE OR REPLACE FUNCTION fn_restrict_assignment_max_score_edit()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.max_score IS DISTINCT FROM OLD.max_score THEN
        IF EXISTS (
            SELECT 1 FROM assignment_submissions
            WHERE assignment_id = OLD.id AND graded_at IS NOT NULL
        ) THEN
            RAISE EXCEPTION 'Không thể đổi điểm tối đa: bài tập đã có bài nộp được chấm';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restrict_assignment_max_score_edit
    BEFORE UPDATE ON assignments
    FOR EACH ROW EXECUTE FUNCTION fn_restrict_assignment_max_score_edit();


-- ----------------------------------------------------------------------------
-- J.4. Sinh thông báo tự động (US-G03)
-- ----------------------------------------------------------------------------

-- (1) Giáo viên nhận thông báo khi có học sinh nộp bài tập
CREATE OR REPLACE FUNCTION fn_notify_new_submission()
RETURNS TRIGGER AS $$
DECLARE
    v_teacher_id UUID;
    v_title TEXT;
BEGIN
    SELECT c.owner_teacher_id,
           u.full_name || ' đã nộp bài: ' || a.title
      INTO v_teacher_id, v_title
      FROM assignments a
      JOIN courses co ON co.id = a.course_id
      JOIN classes c  ON c.id  = co.class_id
      JOIN users u    ON u.id  = NEW.student_id
     WHERE a.id = NEW.assignment_id;

    INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
    VALUES (v_teacher_id, 'NEW_SUBMISSION', v_title, 'assignment_submission', NEW.id);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_new_submission
    AFTER INSERT ON assignment_submissions
    FOR EACH ROW EXECUTE FUNCTION fn_notify_new_submission();

-- (2) Học sinh nhận thông báo khi bài tập được chấm
CREATE OR REPLACE FUNCTION fn_notify_assignment_graded()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.graded_at IS NULL AND NEW.graded_at IS NOT NULL THEN
        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        VALUES (
            NEW.student_id,
            'ASSIGNMENT_GRADED',
            'Bài tập của bạn đã được chấm điểm',
            'assignment_submission',
            NEW.id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_assignment_graded
    AFTER UPDATE ON assignment_submissions
    FOR EACH ROW EXECUTE FUNCTION fn_notify_assignment_graded();

-- (3) Học sinh nhận thông báo khi bài học được xuất bản
CREATE OR REPLACE FUNCTION fn_notify_lesson_published()
RETURNS TRIGGER AS $$
DECLARE
    v_class_id UUID;
BEGIN
    IF OLD.status = 'DRAFT' AND NEW.status = 'PUBLISHED' THEN
        SELECT co.class_id INTO v_class_id
        FROM courses co WHERE co.id = NEW.course_id;

        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        SELECT cm.student_id, 'NEW_LESSON_PUBLISHED',
               'Bài học mới: ' || NEW.title, 'lesson', NEW.id
        FROM class_members cm
        WHERE cm.class_id = v_class_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_lesson_published
    AFTER UPDATE ON lessons
    FOR EACH ROW EXECUTE FUNCTION fn_notify_lesson_published();

-- (4) Học sinh nhận thông báo khi bài tập được xuất bản
CREATE OR REPLACE FUNCTION fn_notify_assignment_published()
RETURNS TRIGGER AS $$
DECLARE
    v_class_id UUID;
BEGIN
    IF OLD.status = 'DRAFT' AND NEW.status = 'PUBLISHED' THEN
        SELECT co.class_id INTO v_class_id
        FROM courses co WHERE co.id = NEW.course_id;

        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        SELECT cm.student_id, 'NEW_ASSIGNMENT_PUBLISHED',
               'Bài tập mới: ' || NEW.title, 'assignment', NEW.id
        FROM class_members cm
        WHERE cm.class_id = v_class_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_assignment_published
    AFTER UPDATE ON assignments
    FOR EACH ROW EXECUTE FUNCTION fn_notify_assignment_published();

-- (4b) Học sinh nhận thông báo khi bài kiểm tra được xuất bản
CREATE OR REPLACE FUNCTION fn_notify_assessment_published()
RETURNS TRIGGER AS $$
DECLARE
    v_class_id UUID;
BEGIN
    IF OLD.status = 'DRAFT' AND NEW.status = 'PUBLISHED' THEN
        SELECT co.class_id INTO v_class_id
        FROM courses co WHERE co.id = NEW.course_id;

        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        SELECT cm.student_id, 'NEW_ASSESSMENT_PUBLISHED',
               'Bài kiểm tra mới: ' || NEW.title, 'assessment', NEW.id
        FROM class_members cm
        WHERE cm.class_id = v_class_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_assessment_published
    AFTER UPDATE ON assessments
    FOR EACH ROW EXECUTE FUNCTION fn_notify_assessment_published();

-- (5) Giáo viên nhận thông báo khi AI xử lý xong (thành công/thất bại)
CREATE OR REPLACE FUNCTION fn_notify_ai_request_done()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'PROCESSING' AND NEW.status = 'COMPLETED' THEN
        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        VALUES (NEW.teacher_id, 'AI_GENERATION_COMPLETED',
                'AI đã tạo xong câu hỏi nháp', 'ai_generation_request', NEW.id);
    ELSIF OLD.status = 'PROCESSING' AND NEW.status = 'FAILED' THEN
        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        VALUES (NEW.teacher_id, 'AI_GENERATION_FAILED',
                'Yêu cầu AI thất bại: ' || COALESCE(NEW.error_message, ''),
                'ai_generation_request', NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_ai_request_done
    AFTER UPDATE ON ai_generation_requests
    FOR EACH ROW EXECUTE FUNCTION fn_notify_ai_request_done();

-- (6) Giáo viên nhận thông báo khi học sinh nộp bài kiểm tra
CREATE OR REPLACE FUNCTION fn_notify_assessment_submitted()
RETURNS TRIGGER AS $$
DECLARE
    v_teacher_id UUID;
BEGIN
    IF OLD.status = 'IN_PROGRESS' AND NEW.status = 'SUBMITTED' THEN
        SELECT c.owner_teacher_id INTO v_teacher_id
        FROM assessments a
        JOIN courses co ON co.id = a.course_id
        JOIN classes c ON c.id = co.class_id
        WHERE a.id = NEW.assessment_id;

        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        VALUES (v_teacher_id, 'NEW_ASSESSMENT_ATTEMPT_SUBMITTED',
                'Có bài kiểm tra cần chấm', 'assessment_attempt', NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_assessment_submitted
    AFTER UPDATE ON assessment_attempts
    FOR EACH ROW EXECUTE FUNCTION fn_notify_assessment_submitted();

-- (7) Học sinh nhận thông báo khi bài kiểm tra được chấm xong
CREATE OR REPLACE FUNCTION fn_notify_assessment_graded()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status <> 'GRADED' AND NEW.status = 'GRADED' THEN
        INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
        VALUES (NEW.student_id, 'ASSESSMENT_GRADED',
                'Bài kiểm tra của bạn đã có điểm', 'assessment_attempt', NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_assessment_graded
    AFTER UPDATE ON assessment_attempts
    FOR EACH ROW EXECUTE FUNCTION fn_notify_assessment_graded();

-- (8) Học sinh nhận thông báo khi có gợi ý học tập mới
CREATE OR REPLACE FUNCTION fn_notify_new_recommendation()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notifications (user_id, type, title, related_entity_type, related_entity_id)
    VALUES (NEW.student_id, 'NEW_RECOMMENDATION',
            'Bạn có gợi ý học tập mới', 'recommendation', NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_new_recommendation
    AFTER INSERT ON recommendations
    FOR EACH ROW EXECUTE FUNCTION fn_notify_new_recommendation();

-- (9) Giáo viên nhận thông báo khi học sinh nộp lại bài (nộp lại là UPDATE nên cần trigger riêng)
CREATE OR REPLACE FUNCTION fn_notify_resubmission()
RETURNS TRIGGER AS $$
DECLARE
    v_teacher_id   UUID;
    v_title        TEXT;
    v_student_name TEXT;
BEGIN
    -- Chỉ xử lý đúng tình huống "nộp lại": khôi phục dòng đã xóa mềm
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        SELECT c.owner_teacher_id, a.title
          INTO v_teacher_id, v_title
          FROM assignments a
          JOIN courses co ON co.id = a.course_id
          JOIN classes c  ON c.id  = co.class_id
         WHERE a.id = NEW.assignment_id;

        SELECT full_name INTO v_student_name FROM users WHERE id = NEW.student_id;

        INSERT INTO notifications (user_id, type, title, content, related_entity_type, related_entity_id)
        VALUES (v_teacher_id, 'SUBMISSION_RESUBMITTED',
                v_student_name || ' đã nộp lại bài tập',
                format('Bài tập "%s" vừa được nộp lại.', v_title),
                'assignment_submission', NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notify_resubmission
    AFTER UPDATE ON assignment_submissions
    FOR EACH ROW
    EXECUTE FUNCTION fn_notify_resubmission();


-- ----------------------------------------------------------------------------
-- J.5. Ràng buộc toàn vẹn chéo bảng
-- ----------------------------------------------------------------------------

-- Chủ đề của bài học phải thuộc cùng khóa học (US-T02A, US-T02)
CREATE OR REPLACE FUNCTION fn_check_lesson_topic_course()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM course_topics WHERE id = NEW.topic_id AND course_id = NEW.course_id
    ) THEN
        RAISE EXCEPTION 'Chủ đề của bài học không thuộc cùng khóa học';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_lesson_topic_course
    BEFORE INSERT OR UPDATE OF topic_id, course_id ON lessons
    FOR EACH ROW EXECUTE FUNCTION fn_check_lesson_topic_course();

-- Khóa học của câu hỏi và mục tiêu học tập liên kết phải trùng nhau (US-T05)
CREATE OR REPLACE FUNCTION fn_check_question_objective_course()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM questions q
          JOIN learning_objectives lo ON lo.id = NEW.objective_id
          JOIN lessons l ON l.id = lo.lesson_id
         WHERE q.id = NEW.question_id
           AND l.course_id = q.course_id
    ) THEN
        RAISE EXCEPTION 'Mục tiêu học tập không thuộc khóa học của câu hỏi';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_question_objective_course
    BEFORE INSERT OR UPDATE OF question_id, objective_id ON question_objectives
    FOR EACH ROW EXECUTE FUNCTION fn_check_question_objective_course();

-- Khóa học của bài tập và mục tiêu học tập liên kết phải trùng nhau (US-T03)
CREATE OR REPLACE FUNCTION fn_check_assignment_objective_course()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM assignments a
          JOIN learning_objectives lo ON lo.id = NEW.objective_id
          JOIN lessons l ON l.id = lo.lesson_id
         WHERE a.id = NEW.assignment_id
           AND l.course_id = a.course_id
    ) THEN
        RAISE EXCEPTION 'Mục tiêu học tập không thuộc khóa học của bài tập';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_assignment_objective_course
    BEFORE INSERT OR UPDATE OF assignment_id, objective_id ON assignment_objectives
    FOR EACH ROW EXECUTE FUNCTION fn_check_assignment_objective_course();

-- Chủ đề hoặc bài học gắn với bài tập phải cùng khóa học (US-T03)
CREATE OR REPLACE FUNCTION fn_check_assignment_scope_course()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.topic_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM course_topics WHERE id = NEW.topic_id AND course_id = NEW.course_id
    ) THEN
        RAISE EXCEPTION 'Chủ đề gắn với bài tập không thuộc cùng khóa học';
    END IF;
    IF NEW.lesson_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM lessons WHERE id = NEW.lesson_id AND course_id = NEW.course_id
    ) THEN
        RAISE EXCEPTION 'Bài học gắn với bài tập không thuộc cùng khóa học';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_assignment_scope_course
    BEFORE INSERT OR UPDATE OF topic_id, lesson_id, course_id ON assignments
    FOR EACH ROW EXECUTE FUNCTION fn_check_assignment_scope_course();

-- Chủ đề hoặc bài học gắn với bài kiểm tra phải cùng khóa học (US-T06)
CREATE OR REPLACE FUNCTION fn_check_assessment_scope_course()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.topic_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM course_topics WHERE id = NEW.topic_id AND course_id = NEW.course_id
    ) THEN
        RAISE EXCEPTION 'Chủ đề gắn với bài kiểm tra không thuộc cùng khóa học';
    END IF;
    IF NEW.lesson_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM lessons WHERE id = NEW.lesson_id AND course_id = NEW.course_id
    ) THEN
        RAISE EXCEPTION 'Bài học gắn với bài kiểm tra không thuộc cùng khóa học';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_assessment_scope_course
    BEFORE INSERT OR UPDATE OF topic_id, lesson_id, course_id ON assessments
    FOR EACH ROW EXECUTE FUNCTION fn_check_assessment_scope_course();

-- Câu hỏi trong bài kiểm tra phải cùng khóa học; không thêm câu hỏi đã xóa mềm (US-T06)
CREATE OR REPLACE FUNCTION fn_check_assessment_question_course()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM questions q
          JOIN assessments a ON a.id = NEW.assessment_id
         WHERE q.id = NEW.question_id
           AND q.course_id = a.course_id
           AND (TG_OP = 'UPDATE' OR q.deleted_at IS NULL)
    ) THEN
        RAISE EXCEPTION 'Câu hỏi không thuộc khóa học của bài kiểm tra hoặc đã bị xóa';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_assessment_question_course
    BEFORE INSERT OR UPDATE OF assessment_id, question_id ON assessment_questions
    FOR EACH ROW EXECUTE FUNCTION fn_check_assessment_question_course();

-- Mục tiêu của bản nháp AI phải thuộc đúng bài học của yêu cầu (US-T09)
CREATE OR REPLACE FUNCTION fn_check_ai_output_objective()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM ai_generation_requests r
          JOIN learning_objectives lo ON lo.lesson_id = r.lesson_id
         WHERE r.id = NEW.request_id
           AND lo.id = NEW.objective_id
    ) THEN
        RAISE EXCEPTION 'Mục tiêu học tập không thuộc bài học của yêu cầu AI';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_ai_output_objective
    BEFORE INSERT OR UPDATE OF request_id, objective_id ON ai_generation_outputs
    FOR EACH ROW EXECUTE FUNCTION fn_check_ai_output_objective();

-- Vai trò: người phụ trách lớp phải là giáo viên, thành viên lớp phải là học sinh
CREATE OR REPLACE FUNCTION fn_check_class_owner_role()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = NEW.owner_teacher_id AND role = 'TEACHER') THEN
        RAISE EXCEPTION 'Người phụ trách lớp phải là tài khoản giáo viên';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_class_owner_role
    BEFORE INSERT OR UPDATE OF owner_teacher_id ON classes
    FOR EACH ROW EXECUTE FUNCTION fn_check_class_owner_role();

CREATE OR REPLACE FUNCTION fn_check_class_member_role()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = NEW.student_id AND role = 'STUDENT') THEN
        RAISE EXCEPTION 'Thành viên lớp phải là tài khoản học sinh';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_class_member_role
    BEFORE INSERT OR UPDATE OF student_id ON class_members
    FOR EACH ROW EXECUTE FUNCTION fn_check_class_member_role();

-- Số lượt làm bài không vượt max_attempts (US-S03)
CREATE OR REPLACE FUNCTION fn_check_max_attempts()
RETURNS TRIGGER AS $$
DECLARE
    v_max  INT;
    v_used INT;
BEGIN
    SELECT max_attempts INTO v_max FROM assessments WHERE id = NEW.assessment_id;
    SELECT COUNT(*) INTO v_used
      FROM assessment_attempts
     WHERE assessment_id = NEW.assessment_id AND student_id = NEW.student_id;
    IF v_used >= v_max THEN
        RAISE EXCEPTION 'Đã dùng hết số lần làm bài (tối đa % lần)', v_max;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_max_attempts
    BEFORE INSERT ON assessment_attempts
    FOR EACH ROW EXECUTE FUNCTION fn_check_max_attempts();

-- Bằng chứng học tập phải trỏ tới nguồn có thật của đúng học sinh và chỉ dành cho mục
-- tiêu được liên kết (US-T04, US-S03)
CREATE OR REPLACE FUNCTION fn_check_evidence_source()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.source_type = 'ASSIGNMENT' THEN
        IF NOT EXISTS (
            SELECT 1 FROM assignment_submissions
             WHERE id = NEW.source_id AND student_id = NEW.student_id
        ) THEN
            RAISE EXCEPTION 'source_id không phải bài nộp của học sinh này';
        END IF;
        IF NOT EXISTS (
            SELECT 1
              FROM assignment_submissions s
              JOIN assignment_objectives ao ON ao.assignment_id = s.assignment_id
             WHERE s.id = NEW.source_id
               AND ao.objective_id = NEW.objective_id
        ) THEN
            RAISE EXCEPTION 'Mục tiêu học tập không được liên kết với bài tập';
        END IF;
    ELSIF NEW.source_type = 'ASSESSMENT' THEN
        IF NOT EXISTS (
            SELECT 1 FROM assessment_attempts
             WHERE assessment_id = NEW.source_id
               AND student_id = NEW.student_id
               AND status = 'GRADED'
        ) THEN
            RAISE EXCEPTION 'source_id không phải bài kiểm tra đã chấm xong của học sinh này';
        END IF;
        IF NOT EXISTS (
            SELECT 1
              FROM assessment_questions aq
              JOIN question_objectives qo ON qo.question_id = aq.question_id
             WHERE aq.assessment_id = NEW.source_id
               AND qo.objective_id = NEW.objective_id
        ) THEN
            RAISE EXCEPTION 'Mục tiêu học tập không được liên kết với câu hỏi nào của bài kiểm tra';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_evidence_source
    BEFORE INSERT OR UPDATE OF student_id, objective_id, source_type, source_id ON learning_evidence
    FOR EACH ROW EXECUTE FUNCTION fn_check_evidence_source();

-- Không mở lại lỗ hổng đã bị giáo viên bỏ qua khi chưa có bằng chứng mới hơn (US-T08, US-H01)
CREATE OR REPLACE FUNCTION fn_block_reopen_dismissed_gap()
RETURNS TRIGGER AS $$
DECLARE
    v_last_dismissed TIMESTAMPTZ;
BEGIN
    IF NEW.status = 'OPEN' THEN
        SELECT MAX(dismissed_at) INTO v_last_dismissed
          FROM knowledge_gaps
         WHERE student_id = NEW.student_id
           AND objective_id = NEW.objective_id
           AND status = 'DISMISSED';

        IF v_last_dismissed IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM learning_evidence e
             WHERE e.student_id = NEW.student_id
               AND e.objective_id = NEW.objective_id
               AND e.updated_at > v_last_dismissed
        ) THEN
            RAISE EXCEPTION 'Lỗ hổng đã được giáo viên bỏ qua, chưa có bằng chứng học tập mới để mở lại';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_reopen_dismissed_gap
    BEFORE INSERT OR UPDATE OF status ON knowledge_gaps
    FOR EACH ROW EXECUTE FUNCTION fn_block_reopen_dismissed_gap();
