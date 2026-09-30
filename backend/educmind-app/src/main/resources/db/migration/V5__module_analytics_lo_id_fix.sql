-- ============================================================================
-- V5__module_analytics_lo_id_fix.sql
-- Chuẩn bị cho Module 5 (Learning Evidence & Analytics).
--
-- Vấn đề phát hiện khi review lại spec Module 5: khóa "objective" trong spec
-- là (lesson_id, learning_objective_code) — nhưng thực tế lessons.learning_objectives
-- (JSONB, Module 3) không có field "code" nào, mỗi objective chỉ có id/text/bloomLevel.
-- Cột questions.learning_objective_code (VARCHAR, Module 4) là 1 ô nhập tự do,
-- KHÔNG hề liên kết thật với id của objective trong lesson — 2 khái niệm đang
-- không nối với nhau trong dữ liệu thật, khác giả định của spec.
--
-- Quyết định (Nghiệp xác nhận, phương án B trong 2 phương án đưa ra): đổi
-- questions.learning_objective_code (VARCHAR tự do) -> questions.learning_objective_id
-- (UUID, tham chiếu đúng "id" của 1 phần tử trong lessons.learning_objectives JSONB
-- của chính lesson đó). Không đặt FK cứng vì objective nằm trong JSONB, không phải
-- bảng riêng (giống cách notifications.source_entity_id xử lý polymorphic reference)
-- — validate "objective_id có thực sự thuộc đúng lesson đó không" ở tầng ứng dụng
-- (QuestionService, qua LessonAccessPort mới).
--
-- Migrate dữ liệu cũ: 2 câu hỏi test hiện có learning_objective_code='LO01', cùng
-- thuộc lesson da1ef213-f5fa-4fa8-991c-62e1d400ced1, lesson này đã có sẵn đúng 1
-- objective thật (id=46744315-2ae0-4e85-948c-1f753bf3f117) -> map thẳng sang id đó,
-- không mất dữ liệu test.
-- ============================================================================

ALTER TABLE questions ADD COLUMN learning_objective_id UUID;

UPDATE questions
SET learning_objective_id = '46744315-2ae0-4e85-948c-1f753bf3f117'
WHERE learning_objective_code = 'LO01';

ALTER TABLE questions DROP COLUMN learning_objective_code;
