-- ============================================================================
-- V3__module_course_subject_and_members.sql
-- THAY ĐỔI KIẾN TRÚC LỚN — xem checkpoint "Course không còn N-N với Class,
-- mỗi Course tự có không gian thành viên riêng" (Nghiệp đã duyệt cả 3 điểm).
--
-- Tóm tắt thay đổi:
--   1. Tạo bảng `subjects` mới — trường tạo sẵn danh sách môn học.
--   2. Bảng `courses`: bỏ cột `subject` (VARCHAR tự do), thay bằng `subject_id`
--      (UUID, NOT NULL, tham chiếu subjects.id).
--   3. Tạo bảng `course_members` mới — mỗi course tự có danh sách thành viên
--      riêng (giáo viên đồng giảng dạy + học sinh), KHÔNG dùng chung
--      class_members của lớp hành chính nữa.
--   4. Xóa hẳn bảng `course_class` (không còn N-N Course-Class).
--
-- Vì sao phải xóa dữ liệu test cũ trước khi đổi schema (đã xác nhận với Nghiệp):
--   Bảng `courses` hiện có 2 dòng test, `course_class` có 1 dòng, `lessons` có
--   2 dòng — toàn bộ dữ liệu này được tạo dưới kiến trúc CŨ (course N-N với
--   class qua course_class, chưa có subject_id/course_members). Dữ liệu này
--   không thể "chuyển đổi" sang model mới một cách có ý nghĩa (vd: không biết
--   gán subject_id nào cho 2 course cũ), nên xóa sạch để bắt đầu lại cho sạch,
--   đúng như dự tính của Nghiệp ở checkpoint mục 9. Resources đang 0 dòng nên
--   không ảnh hưởng, xóa cho chắc/nhất quán với thứ tự dọn dẹp.
--
-- Thứ tự DELETE bắt buộc theo FK (xóa con trước, cha sau):
--   resources -> lessons -> course_class -> courses
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. Dọn dữ liệu test cũ (thuộc kiến trúc cũ, không còn ý nghĩa dưới model mới)
-- ----------------------------------------------------------------------------
DELETE FROM resources;
DELETE FROM lessons;
DELETE FROM course_class;
DELETE FROM courses;

-- ----------------------------------------------------------------------------
-- 1. subjects — môn học do SCHOOL_ADMIN tạo sẵn cho trường mình
-- ----------------------------------------------------------------------------
CREATE TABLE subjects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(20),
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_subjects_school_name UNIQUE (school_id, name)
);

CREATE INDEX idx_subjects_school_id ON subjects(school_id);
CREATE INDEX idx_subjects_deleted_at ON subjects(deleted_at);

-- ----------------------------------------------------------------------------
-- 2. courses — bỏ cột subject (VARCHAR tự do), thêm subject_id (bắt buộc)
-- ----------------------------------------------------------------------------
-- Vì bảng courses đã được xóa sạch dữ liệu ở bước 0, thêm cột NOT NULL trực
-- tiếp là an toàn (không có dòng cũ nào vi phạm ràng buộc).
ALTER TABLE courses DROP COLUMN subject;
ALTER TABLE courses ADD COLUMN subject_id UUID NOT NULL REFERENCES subjects(id);

CREATE INDEX idx_courses_subject_id ON courses(subject_id);

-- ----------------------------------------------------------------------------
-- 3. course_members — không gian thành viên riêng của từng course
-- ----------------------------------------------------------------------------
-- source_class_id: lớp hành chính (bảng classes, Module 1 Phần B) mà học sinh
-- này được chọn ra khi thêm vào course — chỉ có ý nghĩa với member_role=STUDENT,
-- NULL với member_role=TEACHER (giáo viên đồng giảng dạy được thêm trực tiếp,
-- không có "lớp hành chính nguồn"). Không đặt CHECK constraint ràng buộc cứng
-- điều này ở DB (vd CHECK (member_role != 'TEACHER' OR source_class_id IS NULL))
-- để tránh phức tạp hóa DDL — validate ở tầng ứng dụng (CourseService) là đủ,
-- nhất quán với cách các module khác validate business rule tại tầng Java.
CREATE TABLE course_members (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id         UUID NOT NULL REFERENCES courses(id),
    user_id           UUID NOT NULL REFERENCES users(id),
    member_role       VARCHAR(20) NOT NULL
                          CHECK (member_role IN ('TEACHER', 'STUDENT')),
    source_class_id   UUID REFERENCES classes(id),
    added_by          UUID NOT NULL REFERENCES users(id),
    added_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at           TIMESTAMPTZ,
    CONSTRAINT uq_course_members_course_user UNIQUE (course_id, user_id)
);

CREATE INDEX idx_course_members_course_id ON course_members(course_id);
CREATE INDEX idx_course_members_user_id ON course_members(user_id);
CREATE INDEX idx_course_members_left_at ON course_members(left_at);

-- ----------------------------------------------------------------------------
-- 4. course_class — xóa hẳn, không còn N-N Course-Class
-- ----------------------------------------------------------------------------
DROP TABLE course_class;
