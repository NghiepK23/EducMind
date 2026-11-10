-- ============================================================================
-- V7 — TRIGGER & RÀNG BUỘC NGHIỆP VỤ Ở TẦNG DB  (Spec v2, mục J)
-- ============================================================================
-- Chia 5 nhóm:
--   J.1 updated_at tự động (9 trigger)
--   J.2 chấm điểm & bài kiểm tra
--   J.3 chỉnh sửa / xóa nội dung
--   J.4 sinh thông báo tự động (9 trigger — tầng ứng dụng KHÔNG tự tạo notification)
--   J.5 toàn vẹn chéo bảng
-- Chạy SAU khi đã có đủ 25 bảng (V1..V6).

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
