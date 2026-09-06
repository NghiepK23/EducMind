# EDUCMIND — AI & RAG (Module 6, 7, 8, 9)

## 1. AI Security Layers (nguyên tắc xuyên suốt)

```
Input Guard → Context Guard → Prompt Guard → Output Guard → Permission Guard
```

| Lớp | Trạng thái | Đã có ở đâu |
|---|---|---|
| Input Guard | `[TBD]` toàn bộ | Chưa module nào đặc tả |
| Context Guard | Tương đối đầy đủ | M6 (retrieval giới hạn `course_id`), M7 (G.5.7) |
| Prompt Guard | `[TBD]` nội dung | Mới có tên Prompt Registry, chưa có constraint cụ thể |
| Output Guard | `[TBD]` danh sách đầy đủ | M8 (H.5.5 — 2 ví dụ tối thiểu: off-topic, trùng đáp án) |
| Permission Guard | Đã `[DECIDED]` | = Resource authorization (ownership model dùng chung) |

**Nguyên tắc bất biến:** AI không tự quyết định mastery/điểm số; mọi output AI phải qua human-in-the-loop trước khi ảnh hưởng dữ liệu chính thức; AI không tự suy diễn nguyên nhân tâm lý/hoàn cảnh học sinh — chỉ diễn giải dựa trên dữ liệu mastery/evidence có sẵn.

---

## 2. Module 6 — RAG Foundation (`ai-worker/`)

### Pipeline (`ai-worker/pipeline/`)
```
Upload → Validation → Storage → Queue → Parse → Structural detection
       → Semantic chunking → Metadata extraction → Embedding → pgvector → READY
```
- Upload/Validation/Storage: đã xử lý ở `module-course` (50MB, whitelist mime-type).
- Queue: công nghệ cụ thể **`[TBD]`** — Redis (tạm) vs RabbitMQ/Kafka.
- Embedding: gọi qua `common.aigateway.AIProvider` — **model/provider 🔴 P0 chưa chốt**, khóa chiều `document_chunks.embedding VECTOR(?)`.
- Kết quả cập nhật ngược `resources.processing_status` (PROCESSING → COMPLETED/FAILED).

### Retrieval (`ai-worker/retrieval/`)
```
Query → Hybrid Search (Vector + Keyword) → Fusion → Reranker → Top-K
```
| Tham số | Trạng thái |
|---|---|
| Kích thước chunk mục tiêu | `[TBD]` đề xuất 200–500 token |
| Thuật toán Fusion | `[TBD]` đề xuất Reciprocal Rank Fusion (RRF) |
| Reranker riêng (cross-encoder) | `[TBD]` |
| Top-K | `[TBD]` |
| Retry policy (lỗi tạm thời) | `[TBD]` số lần + backoff |
| Reprocess policy (resource đã COMPLETED) | `[TBD]` |
| OCR/transcript ảnh-audio-video | `[TBD]`, ngoài MVP |

### Endpoint nội bộ duy nhất
`POST /api/v1/internal/retrieval/search` — nhận `query, course_id, lesson_id?`, trả Top-K chunk. Chỉ gọi từ `module-teacher-assistant`/`module-tutor`/`module-personalization`.

---

## 3. Module 7 — AI Teacher Assistant (`module-teacher-assistant/`)

**Prompt Registry cần:** `lesson_plan_v1`, `question_generation_v1`, `assessment_matrix_v1`.

**Luồng Question Bank Review Gate (đã chốt, quan trọng — dễ hiểu sai):**
```
AI sinh nháp → lưu ai_generation_outputs (review_status=PENDING_REVIEW)
   → giáo viên APPROVE/EDIT_AND_APPROVE
   → HỆ THỐNG MỚI INSERT vào bảng `questions` của module-assessment
     (source='AI_GENERATED', review_status='APPROVED' set cứng ngay lúc insert)
   → KHÔNG BAO GIỜ có dòng `questions` ở trạng thái PENDING từ luồng AI
```
Lý do: tránh 2 lớp gate trùng nhau (`ai_generation_outputs.review_status` + `questions.review_status`).

**Rate limit:** 28 request/ngày/giáo viên (gộp cả 3 loại LESSON_PLAN/QUESTION_SET/ASSESSMENT_MATRIX).

**Còn mở:** model selection theo request_type, timeout tối đa, cho phép "regenerate" hay không.

---

## 4. Module 8 — AI Tutor (`module-tutor/`)

**Prompt Registry cần:** `ai_tutor_v1`.

**Nguyên tắc cốt lõi:** `Student thinking > Immediate answer`.

**4 cấp Scaffolding:**
```
Level 1 → Hint (gợi ý nhỏ)
Level 2 → Guiding question (câu hỏi dẫn dắt)
Level 3 → Concept explanation (giải thích khái niệm)     ← 🔴 ranh giới với Level 2 CHƯA CHỐT
Level 4 → Worked example (ví dụ khác câu hỏi gốc, KHÔNG trùng)
```

Quy tắc tăng cấp: mặc định Level 1 → escalate khi học sinh hỏi lại/yêu cầu rõ hơn → tuần tự 1→2→3→4, không nhảy vượt cấp, trừ ngoại lệ: mastery thấp/có gap OPEN → cho khởi điểm Level 2.

**🔴 P0 — CHƯA CHỐT:** nội dung cụ thể phân biệt Level 2 vs Level 3 trong prompt thật. Team AI Tutor **code khung API/DB trước**, phần build prompt thật chờ dữ liệu sư phạm.

**Guardrail bắt buộc (không tắt được):** nếu học sinh có `assessment_attempts.status=IN_PROGRESS` cho course đang chat → chặn trích dẫn đề thi/Level 3-4, trả `TUTOR_UNAVAILABLE_DURING_ASSESSMENT`, KHÔNG gọi AI Gateway.

**Rate limit:** 70 tin nhắn/ngày/học sinh + tối đa 3 hội thoại `ACTIVE` đồng thời.

**Còn mở:** độ dài lịch sử hội thoại gửi vào prompt, streaming hay không, idle timeout, danh sách Output Guard đầy đủ, ngôn ngữ trả lời, Tutor có sinh `learning_evidence` không (đề xuất: KHÔNG).

---

## 5. Module 9 — Personalization (`module-personalization/`)

**Prompt Registry cần:** `personalization_draft_v1`, `insight_report_v1`.

**Khi nào trigger `AI_DRAFT`** (thay vì chỉ dùng RULE_ENGINE của M5) — tiêu chí đề xuất, `[TBD]` chưa kiểm chứng:
```
a. Học sinh dismiss >= 2 recommendation RULE_ENGINE liên tiếp cùng gap, HOẶC
b. knowledge_gap liên quan >= 2 objective cùng lúc bị OPEN trong cùng 1 lesson
```

**Học sinh KHÔNG thấy** `recommendation` khi `generated_by='AI_DRAFT'` cho tới khi `review_status ∈ {APPROVED, EDITED_AND_APPROVED}` — filter bắt buộc ở API `GET /students/me/recommendations`.

**Thuật toán sắp `learning_path` (đã chốt, không còn dùng cột `priority`):**
```
1. mastery_score thấp hơn → ưu tiên trước
2. Nếu bằng nhau: knowledge_gap.opened_at cũ hơn → ưu tiên trước
3. REATTEMPT_ASSESSMENT ưu tiên thấp hơn REVIEW_CONTENT/PRACTICE_TARGETED cùng mức
```

**Vòng lặp Student Action → New Evidence:** Module 9 **KHÔNG tạo `learning_evidence` trực tiếp** — chỉ điều hướng UI (vd trỏ tới resource, hoặc trỏ tới màn hình làm lại bài). Evidence mới chỉ đến khi học sinh thật sự làm ASSIGNMENT/ASSESSMENT qua luồng Module 4 có sẵn.

**Rate limit:** 7 insight report/ngày/giáo viên. (Luồng `AI_DRAFT` tự động không tính vào rate-limit này — chưa có quota riêng, `[TBD]`.)

---

## 6. AI Observability & Rate Limit (dùng chung `common.aigateway`)

```
Mọi lời gọi AIProvider.generate()/generateStructured()
        ↓
Ghi 1 dòng vào ai_observability_logs (request_context_type, request_context_id, latency, cost, status)
        ↓
AiQuotaService.checkAndIncrement() đếm theo created_at, group theo teacher_id/student_id, cửa sổ 24h
        ↓
Vượt quota → 429 RATE_LIMIT_EXCEEDED (trước khi gọi AI Gateway thật, tránh tốn chi phí)
```

| Nhánh | Đơn vị | Giá trị MVP |
|---|---|---|
| M7 Teacher Assistant | request/ngày/giáo viên | 28 |
| M8 AI Tutor | tin nhắn/ngày/học sinh | 70 |
| M8 AI Tutor | hội thoại ACTIVE đồng thời | 3 |
| M9 Personalization | insight report/ngày/giáo viên | 7 |

## Tài liệu liên quan
[`ARCHITECTURE.md`](./ARCHITECTURE.md) · [`DATABASE.md`](./DATABASE.md) · [`API.md`](./API.md) · [`CODING_RULES.md`](./CODING_RULES.md)
