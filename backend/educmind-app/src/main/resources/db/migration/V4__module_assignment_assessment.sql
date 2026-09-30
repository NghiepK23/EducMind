-- ============================================================================
-- V4__module_assignment_assessment.sql
-- Module 4 — Assignment (PHẦN D) + Assessment/Question Bank (PHẦN E).
--
-- Đã điều chỉnh theo kiến trúc course_members mới (xem V3 + checkpoint
-- "Course không còn N-N với Class"): KHÔNG có assignment_class/assessment_class
-- như spec gốc — mỗi course đã tự có đúng 1 nhóm thành viên cố định
-- (course_members), nên publish assignment/assessment áp dụng ngay cho toàn bộ
-- học sinh đang active (left_at IS NULL, member_role='STUDENT') trong course đó,
-- không cần bảng nối theo từng lớp nữa (Nghiệp đã xác nhận).
--
-- opens_at/closes_at (vốn nằm ở assessment_class trong spec gốc, mỗi lớp 1 mốc
-- riêng) nay chuyển thành 2 cột trực tiếp trên assessments — dùng chung cho
-- toàn bộ học sinh trong course, vì course chỉ còn đúng 1 nhóm học sinh duy
-- nhất (Nghiệp đã xác nhận: mốc thời gian mở/đóng bài không thể tách riêng
-- theo từng học sinh).
--
-- questions.lesson_id: giữ nguyên theo Module04_PATCH_lesson_id.md — cặp
-- (lesson_id, learning_objective_code) là khóa xác định objective, vì code
-- (vd "LO01") chỉ duy nhất trong phạm vi 1 lesson, không phải toàn course.
--
-- Question Bank Review Gate (PATCH_4Decisions.md PATCH 1): questions với
-- source='AI_GENERATED' chỉ được ghi vào bảng SAU khi đã duyệt ở Module 7 —
-- set thẳng review_status='APPROVED' ngay lúc insert, không đi qua PENDING.
-- Cột review_status vẫn giữ đủ 3 giá trị để tương thích luồng MANUAL.
-- Module 7 chưa code, không ảnh hưởng gì tới migration này.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. questions — ngân hàng câu hỏi
-- ----------------------------------------------------------------------------
CREATE TABLE questions (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id                 UUID NOT NULL REFERENCES courses(id),
    lesson_id                 UUID REFERENCES lessons(id),
    question_text             TEXT NOT NULL,
    question_type             VARCHAR(30) NOT NULL
                                   CHECK (question_type IN
                                       ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE',
                                        'SHORT_ANSWER', 'ESSAY')),
    difficulty                VARCHAR(20) NOT NULL
                                   CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    cognitive_level           VARCHAR(30)
                                   CHECK (cognitive_level IN
                                       ('REMEMBER', 'UNDERSTAND', 'APPLY',
                                        'ANALYZE', 'EVALUATE', 'CREATE')),
    learning_objective_code   VARCHAR(50),
    options                   JSONB,
    correct_answer            JSONB,
    explanation               TEXT,
    source                    VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
                                   CHECK (source IN ('MANUAL', 'AI_GENERATED')),
    review_status             VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
                                   CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_by                UUID NOT NULL REFERENCES users(id),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ
);

CREATE INDEX idx_questions_course_id ON questions(course_id);
CREATE INDEX idx_questions_lesson_id ON questions(lesson_id);
CREATE INDEX idx_questions_deleted_at ON questions(deleted_at);

-- ----------------------------------------------------------------------------
-- 2. assignments — hoạt động học tập không chấm điểm tự động
-- ----------------------------------------------------------------------------
-- KHÔNG có assignment_class (xem ghi chú đầu file) — publish_status=PUBLISHED
-- là đủ điều kiện để mọi học sinh active trong course_members của course đó
-- nhìn thấy, không cần gắn riêng với lớp nào.
CREATE TABLE assignments (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id                UUID NOT NULL REFERENCES courses(id),
    lesson_id                UUID REFERENCES lessons(id),
    title                    VARCHAR(255) NOT NULL,
    description              TEXT,
    due_date                 TIMESTAMPTZ,
    allow_late_submission    BOOLEAN NOT NULL DEFAULT false,
    publish_status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                                  CHECK (publish_status IN ('DRAFT', 'PUBLISHED')),
    created_by               UUID NOT NULL REFERENCES users(id),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ
);

CREATE INDEX idx_assignments_course_id ON assignments(course_id);
CREATE INDEX idx_assignments_lesson_id ON assignments(lesson_id);
CREATE INDEX idx_assignments_deleted_at ON assignments(deleted_at);

-- ----------------------------------------------------------------------------
-- 3. assignment_submissions — bài nộp của học sinh
-- ----------------------------------------------------------------------------
-- UNIQUE(assignment_id, student_id): chỉ nộp được đúng 1 lần, không cho sửa/nộp
-- lại kể cả trước deadline (Module04_FINAL.md, mục D.5.3 — [DECIDED]).
CREATE TABLE assignment_submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID NOT NULL REFERENCES assignments(id),
    student_id      UUID NOT NULL REFERENCES users(id),
    text_answer     TEXT,
    resource_id     UUID REFERENCES resources(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
                        CHECK (status IN ('NOT_STARTED', 'SUBMITTED', 'LATE')),
    submitted_at    TIMESTAMPTZ,
    score           NUMERIC(5,2),
    feedback        TEXT,
    graded_by       UUID REFERENCES users(id),
    graded_at       TIMESTAMPTZ,
    CONSTRAINT uq_assignment_submissions_assignment_student UNIQUE (assignment_id, student_id)
);

CREATE INDEX idx_assignment_submissions_assignment_id ON assignment_submissions(assignment_id);
CREATE INDEX idx_assignment_submissions_student_id ON assignment_submissions(student_id);

-- ----------------------------------------------------------------------------
-- 4. assessments — đề kiểm tra
-- ----------------------------------------------------------------------------
-- opens_at/closes_at: [MỚI so với spec gốc] chuyển từ assessment_class xuống
-- đây, dùng chung cho toàn bộ học sinh trong course (xem ghi chú đầu file).
-- KHÔNG có assessment_class.
CREATE TABLE assessments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id           UUID NOT NULL REFERENCES courses(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    duration_minutes    INT,
    max_attempts        INT NOT NULL DEFAULT 1,
    blueprint_config    JSONB,
    opens_at            TIMESTAMPTZ,
    closes_at           TIMESTAMPTZ,
    publish_status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                             CHECK (publish_status IN ('DRAFT', 'PUBLISHED')),
    published_at        TIMESTAMPTZ,
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_assessments_course_id ON assessments(course_id);
CREATE INDEX idx_assessments_deleted_at ON assessments(deleted_at);

-- ----------------------------------------------------------------------------
-- 5. assessment_questions — câu hỏi thuộc 1 đề kiểm tra
-- ----------------------------------------------------------------------------
CREATE TABLE assessment_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id   UUID NOT NULL REFERENCES assessments(id),
    question_id     UUID NOT NULL REFERENCES questions(id),
    order_index     INT NOT NULL DEFAULT 0,
    score_weight    NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    CONSTRAINT uq_assessment_questions_assessment_question UNIQUE (assessment_id, question_id)
);

CREATE INDEX idx_assessment_questions_assessment_id ON assessment_questions(assessment_id);
CREATE INDEX idx_assessment_questions_question_id ON assessment_questions(question_id);

-- ----------------------------------------------------------------------------
-- 6. assessment_attempts — lượt làm bài của học sinh
-- ----------------------------------------------------------------------------
CREATE TABLE assessment_attempts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id     UUID NOT NULL REFERENCES assessments(id),
    student_id        UUID NOT NULL REFERENCES users(id),
    attempt_number    INT NOT NULL DEFAULT 1,
    status            VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                           CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED')),
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at      TIMESTAMPTZ,
    total_score       NUMERIC(6,2),
    CONSTRAINT uq_assessment_attempts_assessment_student_number
        UNIQUE (assessment_id, student_id, attempt_number)
);

CREATE INDEX idx_assessment_attempts_assessment_id ON assessment_attempts(assessment_id);
CREATE INDEX idx_assessment_attempts_student_id ON assessment_attempts(student_id);

-- ----------------------------------------------------------------------------
-- 7. submission_answers — câu trả lời của học sinh trong 1 lượt làm bài
-- ----------------------------------------------------------------------------
CREATE TABLE submission_answers (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id         UUID NOT NULL REFERENCES assessment_attempts(id),
    question_id        UUID NOT NULL REFERENCES questions(id),
    student_answer     JSONB,
    is_correct         BOOLEAN,
    score_awarded      NUMERIC(5,2),
    teacher_feedback   TEXT,
    CONSTRAINT uq_submission_answers_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_submission_answers_attempt_id ON submission_answers(attempt_id);
CREATE INDEX idx_submission_answers_question_id ON submission_answers(question_id);
