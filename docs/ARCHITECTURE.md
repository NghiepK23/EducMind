# EDUCMIND — ARCHITECTURE (Kiến trúc hệ thống)

## 1. Kiểu kiến trúc

**Modular Monolith** (Spring Boot) cho toàn bộ nghiệp vụ + **1 AI Worker riêng** (Python) cho pipeline RAG/embedding — `[DECIDED]` theo tài liệu kiến trúc gốc.

```
                        ┌─────────────────────────────┐
                        │        CLIENT (FE)          │
                        │  Web/App — polling 30–60s    │
                        └───────────────┬─────────────┘
                                        │ REST — envelope {success,data,error,meta}
              ┌─────────────────────────┴─────────────────────────┐
              │            SPRING BOOT MODULAR MONOLITH             │
              │                                                     │
              │  module-auth          module-schoolclass            │
              │  module-course        module-assignment             │
              │  module-assessment    module-analytics               │
              │  module-teacher-assistant   module-tutor              │
              │  module-personalization     module-notification       │
              │  module-reports                                       │
              │                                                        │
              │  Giao tiếp nội bộ: Spring ApplicationEvent             │
              │  (KHÔNG REST nội bộ, trừ gọi sang AI Worker)           │
              └───────────────┬─────────────────────┬──────────────────┘
                              │                       │
                 ┌────────────┴───────────┐   ┌───────┴────────────┐
                 │   AI WORKER (Python)    │   │   POSTGRESQL         │
                 │   ai-worker/            │   │   + pgvector          │
                 │   pipeline / retrieval  │   │   (1 database)         │
                 │   AI Gateway/Provider   │   └─────────────────────────┘
                 │   (Gemini/OpenAI/Local) │
                 └────────────┬────────────┘
                              │
                 ┌────────────┴───────────┐
                 │   REDIS                │
                 │   cache + hàng đợi     │
                 │   (công nghệ hàng đợi  │
                 │   chính thức: [TBD])   │
                 └─────────────────────────┘
```

## 2. Nguyên tắc kiến trúc bắt buộc

1. **Envelope response chung:** mọi API trả về `{success, data, error, meta}` — không có ngoại lệ.
2. **Giao tiếp nội module:** chỉ qua `ApplicationEventPublisher` (Spring), KHÔNG gọi Service của module khác trực tiếp bằng Java import — trừ đọc qua Repository interface `readonly` đã export tường minh (xem mục 4).
3. **Giao tiếp Monolith ↔ AI Worker:** REST nội bộ (`POST /api/v1/internal/retrieval/search`) — endpoint duy nhất được phép gọi trực tiếp xuyên "boundary" theo kiểu network call.
4. **Ownership 2 tầng:** `owner` (created_by) vs `đồng giảng dạy` — implement 1 lần trong `educmind-common`, mọi module tái sử dụng (xem `CODING_RULES.md`).
5. **3 chuẩn pagination cố định**, không được tự sáng tạo chuẩn thứ 4 (xem `API.md` mục Pagination).
6. **AI Observability bắt buộc:** mọi lời gọi AI Gateway (M7/M8/M9) phải ghi `ai_observability_logs` — đây vừa là log chi phí, vừa là nguồn đếm rate-limit (không có bảng quota riêng).
7. **Human-in-the-loop:** mọi nội dung AI sinh ra đi qua state machine `PENDING_REVIEW → APPROVED/REJECTED/EDITED_AND_APPROVED` trước khi ảnh hưởng dữ liệu chính thức.

## 3. Bản đồ phụ thuộc & thứ tự phát triển

```
Lớp 0 (nền tảng)         module-auth, module-schoolclass
Lớp 1                    module-course
Lớp 2                    module-assignment, module-assessment  |  ai-worker (RAG)
Lớp 3                    module-analytics                      |  module-teacher-assistant
Lớp 4                    module-tutor
Lớp 5                    module-personalization
Lớp 6 (subscriber)       module-notification, module-reports
Xuyên suốt               Security / Performance / Monitoring / Deployment / AI Evaluation
```

Quy tắc: **không code Lớp N trước khi schema của các module ở Lớp < N đã "đóng băng" (frozen)** — chỉ được `ALTER TABLE` thêm cột nullable sau này (giống pattern `Module04_PATCH_lesson_id`, `Module09 PATCH recommendation`), không sửa/xóa cột đã có.

## 4. Ranh giới module & cách đọc dữ liệu chéo

| Module đọc | Đọc dữ liệu của | Cách đọc | Được sửa không? |
|---|---|---|---|
| module-tutor (M8) | mastery, knowledge_gap (M5) | Repository interface `readonly` export từ module-analytics | Không — chỉ đọc |
| module-tutor (M8) | assessment_attempts (M4) | Repository interface `readonly` export từ module-assessment | Không — chỉ đọc |
| module-teacher-assistant (M7) | document_chunks (M6/ai-worker) | REST nội bộ `/internal/retrieval/search` | Không |
| module-personalization (M9) | mastery, knowledge_gap, recommendation (M5) | Đọc trực tiếp (cùng bounded context nghiệp vụ) + PATCH thêm cột vào `recommendation` | Chỉ thêm cột, không sửa cột cũ |
| module-notification (M10) | Mọi module (M4/M5/M7/M8/M9) | Application Event (subscriber thuần túy) | Không bao giờ ghi ngược |
| module-reports (M10) | courses/lessons (M3), assignment/assessment (M4), mastery/gap (M5), ai_generation_requests (M7), recommendation/insight_reports (M9) | Query trực tiếp (read-only, tổng hợp) | Không |

## 5. Thành phần dùng chung (`educmind-common`)

Bắt buộc code **trước tiên**, mọi module import — chi tiết implement xem `CODING_RULES.md`:

| Package | Chức năng |
|---|---|
| `common.envelope` | `ApiResponse<T>`, `ErrorCode`, `@ControllerAdvice` xử lý lỗi tập trung |
| `common.pagination` | 3 util: `OffsetPageUtil`, `CursorMessageUtil`, `CursorSequenceUtil` |
| `common.authorization` | `CourseAuthorizationService.isOwnerOrCoTeacher()`, `RoleGuard` |
| `common.events` | Toàn bộ Application Event class dùng xuyên module |
| `common.aigateway` | `AIProvider` interface, `PromptRegistry`, `AiQuotaService` |
| `common.review` | Generic reviewable state machine (`PENDING_REVIEW → APPROVED/REJECTED/EDITED_AND_APPROVED`) |
| `common.exception` | Exception class chuẩn + mapping sang HTTP status/error code |

## 6. AI Worker (Python) — chi tiết xem `AI_RAG.md`

```
ai-worker/
├── pipeline/        # Upload→Parse→Chunk→Embed (Module 6)
├── retrieval/        # Hybrid search + Fusion + Reranker
├── queue_consumer/   # Lắng nghe queue xử lý resource mới
└── prompt_registry/  # lesson_plan_v1, question_generation_v1, ai_tutor_v1, insight_report_v1...
```

## 7. Migration order (Flyway) — chi tiết xem `DATABASE.md`

```
V1  module-auth + module-schoolclass
V2  module-course
V3  module-assignment + module-assessment
V4  ai-worker (document_chunks — chờ chốt VECTOR(?))
V5  module-analytics
V6  module-teacher-assistant
V7  module-tutor
V8  module-personalization (PATCH recommendation)
V9  module-notification + module-reports
V10 cross-cutting (ai_observability_logs, audit_log)
```

## 8. Rủi ro kiến trúc cần giám sát

| Rủi ro | Giảm thiểu |
|---|---|
| 2 module cùng sửa bảng `recommendation` (M5 tạo, M9 patch) | Freeze schema M5 trước, M9 chỉ ALTER thêm cột |
| Module đọc chéo bằng SQL join trực tiếp thay vì qua interface | Code review chặn import Repository của module khác ngoài interface `readonly` đã khai báo |
| Rate-limit bị đếm sai do mỗi module tự viết logic riêng | Bắt buộc dùng `AiQuotaService` chung |
| Pagination nhầm chuẩn (offset cho tin nhắn, cursor cho danh sách hội thoại) | Checklist review PR đối chiếu `API.md` mục Pagination |

## 9. Tài liệu liên quan
[`REQUIREMENTS.md`](./REQUIREMENTS.md) · [`DATABASE.md`](./DATABASE.md) · [`API.md`](./API.md) · [`AI_RAG.md`](./AI_RAG.md) · [`CODING_RULES.md`](./CODING_RULES.md)
