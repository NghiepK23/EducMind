-- Module 5: Learning Evidence & Analytics
-- Khóa objective = (lesson_id, learning_objective_id). learning_objective_id là id
-- của 1 phần tử trong lessons.learning_objectives (JSONB), nên không có FK cứng.
-- Không có cột recommendation.priority (đã bỏ theo spec Module 5 FINAL).

CREATE TABLE learning_evidence (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id             UUID NOT NULL REFERENCES users(id),
    class_id               UUID NOT NULL REFERENCES classes(id),
    course_id              UUID NOT NULL REFERENCES courses(id),
    lesson_id              UUID NULL REFERENCES lessons(id),
    learning_objective_id  UUID NULL,
    source_type            VARCHAR(20) NOT NULL,
    source_id              UUID NOT NULL,
    score                  NUMERIC(5,4) NOT NULL,
    raw_score              NUMERIC(6,2) NULL,
    weight                 NUMERIC(3,2) NOT NULL DEFAULT 1.00,
    occurred_at            TIMESTAMPTZ NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NULL,
    CONSTRAINT chk_evidence_source_type CHECK (source_type IN ('ASSIGNMENT', 'ASSESSMENT', 'REASSESSMENT')),
    CONSTRAINT chk_evidence_score_range CHECK (score >= 0 AND score <= 1),
    CONSTRAINT chk_evidence_objective_needs_lesson CHECK (learning_objective_id IS NULL OR lesson_id IS NOT NULL)
);

CREATE UNIQUE INDEX uq_learning_evidence_source_objective
    ON learning_evidence (
        source_type,
        source_id,
        COALESCE(lesson_id, '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(learning_objective_id, '00000000-0000-0000-0000-000000000000'::uuid)
    );

CREATE INDEX idx_learning_evidence_student_objective
    ON learning_evidence (student_id, lesson_id, learning_objective_id);
CREATE INDEX idx_learning_evidence_source
    ON learning_evidence (source_type, source_id);

CREATE TABLE mastery (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id             UUID NOT NULL REFERENCES users(id),
    lesson_id              UUID NOT NULL REFERENCES lessons(id),
    learning_objective_id  UUID NOT NULL,
    course_id              UUID NOT NULL REFERENCES courses(id),
    mastery_score          NUMERIC(5,4) NOT NULL,
    evidence_count         INT NOT NULL DEFAULT 0,
    confidence_level       VARCHAR(10) NOT NULL DEFAULT 'LOW',
    last_evidence_at       TIMESTAMPTZ NULL,
    calculation_method     VARCHAR(50) NOT NULL DEFAULT 'RULE_V1_WEIGHTED_AVG_DECAY',
    computed_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mastery_student_objective UNIQUE (student_id, lesson_id, learning_objective_id)
);

CREATE INDEX idx_mastery_course ON mastery (course_id, student_id);

CREATE TABLE mastery_history (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mastery_id             UUID NOT NULL REFERENCES mastery(id),
    mastery_score          NUMERIC(5,4) NOT NULL,
    snapshot_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    trigger_evidence_id    UUID NOT NULL REFERENCES learning_evidence(id)
);

CREATE INDEX idx_mastery_history_mastery ON mastery_history (mastery_id, snapshot_at DESC);

CREATE TABLE knowledge_gap (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id             UUID NOT NULL REFERENCES users(id),
    lesson_id              UUID NOT NULL REFERENCES lessons(id),
    learning_objective_id  UUID NOT NULL,
    course_id              UUID NOT NULL REFERENCES courses(id),
    status                 VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    opened_reason          VARCHAR(500) NOT NULL,
    resolved_at            TIMESTAMPTZ NULL,
    resolved_reason        VARCHAR(500) NULL,
    dismissed_by           UUID NULL REFERENCES users(id),
    dismissed_reason       TEXT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_gap_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);

CREATE UNIQUE INDEX uq_knowledge_gap_open
    ON knowledge_gap (student_id, lesson_id, learning_objective_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_knowledge_gap_course_status ON knowledge_gap (course_id, status);

CREATE TABLE recommendation (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id             UUID NOT NULL REFERENCES users(id),
    lesson_id              UUID NOT NULL REFERENCES lessons(id),
    learning_objective_id  UUID NOT NULL,
    course_id              UUID NOT NULL REFERENCES courses(id),
    knowledge_gap_id       UUID NULL REFERENCES knowledge_gap(id),
    type                   VARCHAR(30) NOT NULL,
    target_resource_id     UUID NULL REFERENCES resources(id),
    generated_by           VARCHAR(20) NOT NULL DEFAULT 'RULE_ENGINE',
    review_status          VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    status                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_recommendation_status CHECK (status IN ('ACTIVE', 'ACCEPTED', 'DISMISSED', 'EXPIRED'))
);

CREATE INDEX idx_recommendation_student_course ON recommendation (student_id, course_id, status);
