-- ============================================================================
-- V2__module_course.sql
-- Module 3 (Course/Lesson/Resource) — Module03_Course_Lesson_Resource_FINAL.md, mục C.4
--
-- Thứ tự CREATE TABLE bắt buộc theo FK: courses -> course_class -> lessons -> resources.
-- Các cột cross-module (school_id, class_id, created_by, uploaded_by, linked_by) có FK
-- constraint THẬT ở DB (REFERENCES bảng gốc bên module khác) — quyết định đã chốt với
-- Nghiệp: dù tầng Java dùng raw UUID (không @ManyToOne, tránh circular Maven dependency
-- ở tầng entity/compile-time), tầng DB vẫn nên ràng buộc referential integrity thật vì
-- toàn hệ thống dùng chung 1 database vật lý (modular monolith, không phải microservices).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. courses
-- ----------------------------------------------------------------------------
CREATE TABLE courses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    subject         VARCHAR(100),
    created_by      UUID NOT NULL REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_courses_school_id ON courses(school_id);
CREATE INDEX idx_courses_created_by ON courses(created_by);
CREATE INDEX idx_courses_deleted_at ON courses(deleted_at);

-- ----------------------------------------------------------------------------
-- 2. course_class (bảng nối N-N)
-- ----------------------------------------------------------------------------
CREATE TABLE course_class (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID NOT NULL REFERENCES courses(id),
    class_id        UUID NOT NULL REFERENCES classes(id),
    linked_by       UUID NOT NULL REFERENCES users(id),
    linked_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    unlinked_at     TIMESTAMPTZ,
    CONSTRAINT uq_course_class UNIQUE (course_id, class_id)
);

CREATE INDEX idx_course_class_course_id ON course_class(course_id);
CREATE INDEX idx_course_class_class_id ON course_class(class_id);

-- ----------------------------------------------------------------------------
-- 3. lessons
-- ----------------------------------------------------------------------------
CREATE TABLE lessons (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id               UUID NOT NULL REFERENCES courses(id),
    title                   VARCHAR(255) NOT NULL,
    description             TEXT,
    learning_objectives     JSONB,
    order_index             INTEGER NOT NULL DEFAULT 0,
    publish_status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                                CHECK (publish_status IN ('DRAFT', 'PUBLISHED')),
    published_at            TIMESTAMPTZ,
    created_by              UUID NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_lessons_course_id ON lessons(course_id);
CREATE INDEX idx_lessons_publish_status ON lessons(publish_status);
CREATE INDEX idx_lessons_deleted_at ON lessons(deleted_at);

-- ----------------------------------------------------------------------------
-- 4. resources
-- ----------------------------------------------------------------------------
CREATE TABLE resources (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id           UUID NOT NULL REFERENCES courses(id),
    lesson_id           UUID REFERENCES lessons(id),
    filename            VARCHAR(255) NOT NULL,
    mime_type           VARCHAR(255) NOT NULL,
    size_bytes          BIGINT NOT NULL,
    storage_key         VARCHAR(512) NOT NULL,
    processing_status   VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
                            CHECK (processing_status IN ('PROCESSING', 'READY', 'FAILED')),
    uploaded_by         UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_resources_course_id ON resources(course_id);
CREATE INDEX idx_resources_lesson_id ON resources(lesson_id);
CREATE INDEX idx_resources_processing_status ON resources(processing_status);
CREATE INDEX idx_resources_deleted_at ON resources(deleted_at);
