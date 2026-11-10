# EDUCMIND — TRẠNG THÁI CHUYỂN ĐỔI SANG SPEC V2

> Nhánh: `arena/e8763d6d-educmind` · Cập nhật: 2026-11-10
> Kế hoạch đầy đủ: [`03-GAP-ANALYSIS.md`](./03-GAP-ANALYSIS.md)

## 1. Quyết định đã chốt

| Vấn đề | Quyết định |
|---|---|
| Module ngoài spec | **Xoá** `module-rag`, `module-reports`, `module-tutor`, `module-personalization` |
| Phiên đăng nhập | **Chỉ access token JWT** (60 phút), không có bảng `refresh_tokens` |
| Sinh thông báo | **Trigger DB** (V7 — J.4); tầng Java chỉ đọc/cập nhật trạng thái đã xem |
| Tiến độ | Làm **Phase 1** trước, review rồi mới sang Phase 2 |

## 2. Phase 1 — ĐÃ XONG

### Schema (Flyway baseline mới, thay toàn bộ V1..V11 cũ)

| File | Nội dung |
|---|---|
| `V1__auth_and_class.sql` | `user_role`, `users`, `password_reset_tokens`, `classes`, `class_members` |
| `V2__course_topics_lessons.sql` | `content_status`, `courses`, `course_topics`, `lessons`, `learning_objectives`, `resources` |
| `V3__assignment.sql` | `submission_status`, `assignments`, `assignment_submissions`, `assignment_objectives` |
| `V4__assessment.sql` | 4 enum + `questions`, `question_objectives`, `assessments`, `assessment_questions`, `assessment_attempts`, `submission_answers` |
| `V5__analytics.sql` | 4 enum + `learning_evidence`, `mastery`, `knowledge_gaps`, `recommendations` |
| `V6__ai_and_notification.sql` | `ai_request_status`, `ai_review_status`, `notification_type` + 4 bảng |
| `V7__triggers.sql` | 25 hàm/trigger của Spec v2 mục J (updated_at, chấm điểm, sửa/xoá, thông báo, toàn vẹn chéo bảng) |

→ Đủ **25 bảng** đúng spec; các bảng của Phase 2/3 đã có sẵn nên không phải sửa migration nữa.

### Code đã viết lại

| Module | Nội dung |
|---|---|
| `module-auth` | `User`, `Role` (2 giá trị), `PasswordResetToken`; login/logout/change-password/reset-password; `GET /users/me`; `UserLookupPortImpl`, `AccountPortImpl` |
| `module-schoolclass` | `SchoolClass` (owner_teacher_id, grade, school_year), `ClassMember` (chỉ học sinh); CRUD lớp; thêm/bớt học sinh bằng email (US-T00) |
| `module-course` | `Course` (class-scoped), `CourseTopic` (US-T02A), `Lesson`, `LearningObjective` (bảng thật), `Resource`; CRUD + publish/unpublish + upload/download tài liệu |
| `module-notification` | Đọc thông báo, `unread-count`, đánh dấu đã đọc / đọc tất cả (US-G03) |
| `educmind-common` | Port mới theo mô hình v2: `ClassAccessPort`, `CourseAccessPort`, `UserLookupPort`, `AccountPort`; bỏ toàn bộ port/event của RAG & notification cũ |
| `educmind-app` | Security 2 role + CORS dev; `application.properties` (JWT_SECRET/DB_PASSWORD qua env); `DevSeedService` + `DevSeedRunner` |

### Đã xoá

`module-rag` (document_chunks, pgvector), `module-reports` (CSV/PDF), `module-tutor`, `module-personalization`,
`backend/ai-worker/` (thư mục rỗng), `database/migrations/V1...` (bản trùng), `subjects`, `schools`,
`invite_codes`, course members/course_class.

## 3. Phase 1 — CỐ Ý CHƯA LÀM (đang "park", code cũ vẫn còn trong repo nhưng KHÔNG build)

| Module | Lý do |
|---|---|
| `module-assignment` | Bảng `assignments/assignment_submissions/assignment_objectives` đã có; code cũ dùng `learning_objective_code` chuỗi + `course_members` nên phải viết lại (US-T03, T04, S02, S07) |
| `module-assessment` | Phải chuyển sang `question_objectives`, JSONB `options/correct_answer`, `max_attempts`, `attempt_number` (US-T05, T06, T13–T15, S03–S04) |
| `module-analytics` | Khoá chuyển từ `(lesson_id, code)` sang `objective_id`; bỏ `mastery_history` (US-T07, T08, S05, S06, H01) |
| `module-teacher-assistant` | Chỉ còn sinh câu hỏi nháp + duyệt (US-T09, T10) |

Các module này **không nằm trong `<modules>` của `backend/pom.xml`** và **không phải dependency của
`educmind-app`** → build hiện tại chỉ gồm 6 module. Khi làm Phase 2/3, bật lại bằng cách thêm lại vào pom
sau khi đã viết lại code.

## 4. Chưa kiểm chứng được trong môi trường này

Máy chạy agent **không có JDK/Maven/PostgreSQL**, nên **chưa chạy `mvn verify` và chưa chạy Flyway thật**.
Cần chạy tại máy bạn:

```bash
docker compose up -d postgres
cd backend && ./mvnw -DskipTests install
EDUCMIND_SEED=true DB_URL=jdbc:postgresql://localhost:5432/educmind \
  ./mvnw -pl educmind-app spring-boot:run
```

Điểm rủi ro cần kiểm tra đầu tiên khi build:
1. Hibernate `validate` với cột kiểu **PG enum** (đã dùng `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`).
2. Trigger J.5 có thể chặn seed nếu thứ tự insert sai (seed đã theo đúng thứ tự: user → class → member → course → topic → lesson → objective).
3. `spring-boot-starter-webmvc` / Spring Boot 4.1.1 (giữ nguyên version của repo gốc).

## 5. Việc còn thiếu so với spec (ngoài Phase 1)

| Mã | Nội dung | Phase |
|---|---|---|
| AC-G03.4 | Thông báo trỏ tới nội dung đã xoá phải hiển thị "không còn khả dụng" | 4 |
| AC-T11.1 | Chặn đổi **mã** mục tiêu khi đã có `learning_evidence` (hiện chặn mọi thay đổi mã) | 3 |
| US-T03/T04 | Bài tập: phạm vi topic/lesson, cửa sổ nộp trễ, chấm lại | 2 |
| US-T05–T15 | Ngân hàng câu hỏi, bài kiểm tra, chấm tay, xem lượt làm | 2 |
| US-T07/T08/S05/S06/H01 | Mastery, lỗ hổng kiến thức, gợi ý | 3 |
| US-T09/T10 | AI sinh câu hỏi nháp + duyệt | 3 |
| — | CI, Testcontainers, test trigger | 5 |
