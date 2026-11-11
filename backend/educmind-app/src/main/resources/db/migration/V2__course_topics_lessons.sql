-- ============================================================================
-- V2 — NHÓM 2: NỘI DUNG DẠY HỌC  (Spec v2, mục D)
-- ============================================================================
-- Cấu trúc: Khóa học -> Chủ đề -> Bài học -> Mục tiêu học tập
-- Thứ tự bắt buộc theo FK: courses -> course_topics -> lessons -> learning_objectives

CREATE TYPE content_status AS ENUM ('DRAFT', 'PUBLISHED');

-- Bảng 4: courses (1 lớp có nhiều khóa học)
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

CREATE INDEX idx_courses_class ON courses(class_id);

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

CREATE INDEX idx_topics_course ON course_topics(course_id);

-- Bảng 6: lessons (thuộc đúng 1 chủ đề của cùng khóa học — xem trigger V7)
CREATE TABLE lessons (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID NOT NULL REFERENCES courses(id),
    topic_id     UUID NOT NULL REFERENCES course_topics(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    status       content_status NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID REFERENCES users(id),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX idx_lessons_course ON lessons(course_id);
CREATE INDEX idx_lessons_topic ON lessons(topic_id);

-- Bảng 7: learning_objectives (mã chỉ duy nhất trong phạm vi 1 bài học)
CREATE TABLE learning_objectives (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id    UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    code         VARCHAR(20) NOT NULL,
    description  TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_objective_code UNIQUE (lesson_id, code)
);

CREATE INDEX idx_lo_lesson ON learning_objectives(lesson_id);

-- Bảng 8: resources (tài liệu của khóa học hoặc của 1 bài học, tối đa 50MB)
CREATE TABLE resources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID REFERENCES courses(id),
    lesson_id       UUID REFERENCES lessons(id),
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0 AND file_size_bytes <= 52428800),
    file_type       VARCHAR(50) NOT NULL,
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_resource_owner CHECK (course_id IS NOT NULL OR lesson_id IS NOT NULL)
);

CREATE INDEX idx_resources_lesson ON resources(lesson_id);
CREATE INDEX idx_resources_course ON resources(course_id);
