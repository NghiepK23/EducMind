# EDUCMIND — REQUIREMENTS (Yêu cầu nghiệp vụ)

> Nguồn: 10 Module FINAL (1, 3–10) + 3 Cross-Module Patch + Master TBD Tracker.
> Tài liệu này trả lời: **hệ thống làm gì, cho ai, phạm vi tới đâu** — chi tiết kỹ thuật (bảng/API) xem `DATABASE.md`/`API.md`.

## 1. Bối cảnh & mục tiêu sản phẩm

EducMind là hệ thống quản lý học tập (LMS) tích hợp AI cho **1 trường học** (quy mô MVP), hỗ trợ:
- Giáo viên soạn nội dung giảng dạy, quản lý lớp/bài tập/đề kiểm tra.
- Học sinh học tập, làm bài, nhận hỗ trợ cá nhân hóa từ AI Tutor.
- Ban giám hiệu (`SCHOOL_ADMIN`) giám sát tổng hợp, cấp tài khoản.
- AI hỗ trợ giáo viên soạn giáo án/câu hỏi (Module 7), hỗ trợ học sinh học (Module 8), và cá nhân hóa lộ trình học (Module 9) — **luôn có con người kiểm soát** (human-in-the-loop), AI không tự quyết định điểm số/mastery.

## 2. Actor & vai trò (RBAC — 4 role)

| Role | Mô tả | Phạm vi |
|---|---|---|
| `SUPER_ADMIN` | Toàn quyền hệ thống | Toàn hệ thống |
| `SCHOOL_ADMIN` | Ban giám hiệu | Trong `school_id` của mình — **chỉ 2 quyền: (1) cấp tài khoản GV/HS, (2) xem số liệu tổng hợp read-only**. Không xem nội dung nghiệp vụ chi tiết (transcript, content AI, report cá nhân học sinh) |
| `TEACHER` | Giáo viên | CRUD nội dung course mình tạo **hoặc đồng giảng dạy** (xem mục 3) |
| `STUDENT` | Học sinh | Chỉ dữ liệu của chính mình, trong phạm vi lớp đã được gán |

## 3. Khái niệm nghiệp vụ cốt lõi cần mọi dev hiểu trước khi code

### 3.1 Ownership Model — "Đồng giảng dạy" (Co-teacher)
- **Owner** = `created_by` của 1 `course`.
- **Đồng giảng dạy** = giáo viên có mặt trong `class_members` (role=TEACHER) của ≥1 `class` đã link với course đó qua `course_class`.
- Đồng giảng dạy có quyền **ngang owner** cho MỌI hành động vận hành hàng ngày (sửa lesson, upload resource, gắn/gỡ lớp, dismiss knowledge gap, review AI output, xem report...).
- Owner giữ **độc quyền duy nhất**: xóa course, đổi `created_by`.
- **Ngoại lệ duy nhất:** ở cấp **Class** (không phải Course), đồng giảng dạy (`class_members` nhưng không phải `owner_teacher_id`) chỉ **xem, không sửa/xóa** lớp — quy tắc này KHÔNG áp dụng quyền ngang nhau như ở Course.
- Module 4 (Assignment/Assessment) hiện **chưa** cập nhật ownership model này — đang giữ "chỉ owner", cần xác nhận nghiệp vụ bổ sung, không chặn code.

### 3.2 Học sinh — cách vào lớp (2 kênh song song)
1. **Chính:** `SCHOOL_ADMIN` tạo tài khoản (`POST /api/v1/users`), kèm `class_id` → tự động vào `class_members`.
2. **Phụ:** `TEACHER` tạo invite code gắn với 1 lớp cụ thể → học sinh tự đăng ký (`POST /api/v1/auth/register`) → tự động vào lớp đó.

### 3.3 Khóa xác định "Learning Objective" — điểm dễ hiểu sai nhất hệ thống
`learning_objective_code` (vd "LO01") **chỉ duy nhất trong phạm vi 1 lesson**, KHÔNG duy nhất trong toàn course. Hai lesson khác nhau cùng course có thể dùng trùng code "LO01" với ý nghĩa hoàn toàn khác nhau.
→ **Khóa chuẩn xuyên hệ thống là cặp `(lesson_id, learning_objective_code)`**, không phải `course_id + code`. Áp dụng cho: `questions`, `learning_evidence`, `mastery`, `knowledge_gap`, `recommendation`.

### 3.4 Vòng lặp Learning Evidence → Mastery → Gap → Recommendation
```
Assignment nộp bài / Assessment nộp bài
        ↓ (event-driven)
Learning Evidence (per lesson_id + objective code)
        ↓ (recompute, có decay theo thời gian)
Mastery (per student, per lesson_id, per objective)
        ↓ (ngưỡng cấu hình)
Knowledge Gap (OPEN/RESOLVED/DISMISSED)
        ↓
Recommendation (RULE_ENGINE mặc định, hoặc AI_DRAFT khi rule không đủ thuyết phục)
        ↓
Learning Path (Module 9 gộp nhiều recommendation thành lộ trình có thứ tự)
        ↓
Học sinh hành động → Evidence mới → lặp lại
```
Nguyên tắc bất di bất dịch: **mastery không do AI quyết định**; AI chỉ diễn giải/gợi ý; mọi ngưỡng số là **cấu hình**, không hard-code; `learning_evidence` **append-only** (chỉ 1 ngoại lệ: update khi giáo viên chấm assignment trễ).

### 3.5 Human-in-the-loop cho mọi nội dung AI sinh ra
Không có nội dung AI nào (giáo án, câu hỏi, recommendation, insight report) được áp dụng vào hệ thống chính thức mà chưa qua giáo viên `APPROVE`/`EDIT_AND_APPROVE`. Không có cơ chế tự động publish.

## 4. Phạm vi theo Module (Scope Matrix)

| # | Module | Trong phạm vi | Ngoài phạm vi |
|---|---|---|---|
| 1 | Auth/School/Class | Đăng nhập, JWT, invite code, RBAC, CRUD trường/lớp | Parent role, SSO ngoài |
| 3 | Course/Lesson/Resource | CRUD course-lesson-resource, upload file ≤50MB | Xử lý nội dung file (RAG) — đó là M6 |
| 4 | Assignment/Assessment | Bài tập không tự chấm, đề kiểm tra tự luận/trắc nghiệm, question bank | — |
| 5 | Learning Evidence/Analytics | Tính mastery, phát hiện gap, sinh recommendation cơ bản | Diễn giải AI (M9), đối thoại (M8) |
| 6 | RAG Foundation | Parse-chunk-embed tài liệu, retrieval hybrid search | Sinh câu trả lời AI (M7/M8) |
| 7 | AI Teacher Assistant | Sinh giáo án/câu hỏi/ma trận đề nháp, review gate | Tương tác học sinh trực tiếp |
| 8 | AI Tutor | Đối thoại học sinh-AI, 4 cấp scaffolding | Tính mastery (chỉ đọc), sinh recommendation |
| 9 | Personalization | AI diễn giải recommendation, learning path, insight report | Tạo evidence trực tiếp, tự tạo assessment attempt |
| 10 | Notification/Reports | Polling notification, báo cáo số liệu tổng hợp | Email/SMS ngoài hệ thống, báo cáo AI văn xuôi (đó là M9) |

## 5. Ràng buộc phi chức năng đã chốt

- MVP quy mô: **1 trường học**, không phải hàng triệu bản ghi → offset pagination đủ dùng cho hầu hết danh sách.
- Notification: **polling 30–60s**, không dùng WebSocket.
- File upload: giới hạn **50MB/file** cho mọi loại (kể cả video/audio — điểm P2 chưa xử lý).
- Rate limit AI (giá trị MVP): 28 request/ngày/GV (M7) · 70 tin nhắn/ngày/HS + 3 hội thoại đồng thời (M8) · 7 insight report/ngày/GV (M9).
- Kiến trúc: Modular Monolith (Spring Boot) + AI Worker riêng (Python).

## 6. Điểm còn mở (P0 — chặn "sẵn sàng production", KHÔNG chặn bắt đầu code)

| # | Điểm | Module | Vì sao chưa chốt |
|---|---|---|---|
| 1 | Embedding model/provider (khóa chiều `VECTOR(?)`) | 6 | Cần quyết định ngân sách/hạ tầng thật |
| 2 | Ranh giới nội dung Level 2 vs Level 3 AI Tutor | 8 | Cần viết prompt thật + kiểm chứng |
| 3 | Ngưỡng số sư phạm (mastery_threshold_low/resolve, half_life_days...) | 5 | Cần dữ liệu vận hành thực tế |

Chi tiết đầy đủ toàn bộ điểm P1–P3 còn lại: xem `Master_TBD_Tracker-2.md` (giữ nguyên trong kho tài liệu gốc, không chép lại ở đây để tránh 2 nguồn sự thật).

## 7. Tài liệu liên quan

- Kiến trúc kỹ thuật: [`ARCHITECTURE.md`](./ARCHITECTURE.md)
- Schema đầy đủ: [`DATABASE.md`](./DATABASE.md)
- Danh sách API: [`API.md`](./API.md)
- Đặc tả AI/RAG: [`AI_RAG.md`](./AI_RAG.md)
- Quy ước code: [`CODING_RULES.md`](./CODING_RULES.md)
