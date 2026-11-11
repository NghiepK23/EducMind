# EDUCMIND — BÁO CÁO NGHIÊN CỨU REPO

> Ngày rà soát: 2026-11-10 · Phạm vi: toàn bộ repo `NghiepK23/EducMind` (nhánh `main`, commit `cb12a22`)
> Phương pháp: đọc mã nguồn + đối chiếu chéo `docs/` với code + kiểm tra metadata GitHub.
> **Chưa kiểm chứng được:** build/test thực tế (sandbox không có JDK/Maven/PostgreSQL), nên mọi kết luận về runtime là suy ra từ đọc code.

---

## 1. Tóm tắt nhanh (Executive summary)

Repo là một **modular monolith Spring Boot cho LMS tích hợp AI** với chất lượng code **trên mức trung bình của đồ án sinh viên**: kiến trúc module hoá rõ ràng, envelope response thống nhất, có phân quyền bằng `@PreAuthorize` + kiểm tra ownership, có refresh-token rotation, có human-in-the-loop review cho output AI, có migration Flyway đánh số đầy đủ.

Tuy nhiên repo **chưa chạy được end-to-end** và có **3 nhóm vấn đề chặn**:

| # | Nhóm vấn đề | Mức độ |
|---|---|---|
| 1 | **Không có cơ chế khởi tạo hệ thống** — không có seed, không có API tạo `school`/`SUPER_ADMIN`, không có CommandLineRunner → DB trắng thì không ai đăng nhập được | 🔴 Chặn chạy |
| 2 | **Lỗ hổng leo thang đặc quyền** ở `POST /api/v1/users` (SCHOOL_ADMIN tạo được cả `SUPER_ADMIN`) + tài khoản tạo ra bị `school_id = NULL` | 🔴 Bảo mật / dữ liệu |
| 3 | **Tài khoản bị DISABLED vẫn đăng nhập và dùng API bình thường** (login + filter đều không check `status`) | 🔴 Bảo mật |
| 4 | **Tài liệu và code đã lệch nhau rõ rệt** (ai-worker Python, thứ tự migration, tên package `common.*`, endpoint thiếu) | 🟠 Nhất quán |
| 5 | **Test & CI gần như trống**: 0 test cho 5/10 module (kể cả `module-analytics` — phần thuật toán khó nhất), không có integration test, không có `.github/workflows` | 🟠 Chất lượng |

Điểm mạnh đáng ghi nhận: cách các service AI được viết (retry, phân loại lỗi, quota, observability log), `HybridRetrievalService` + RRF + tsvector an toàn injection, và các quyết định trong code đều kèm comment giải thích "vì sao".

---

## 2. Sự thật về repo (facts)

| Hạng mục | Giá trị |
|---|---|
| GitHub | `NghiepK23/EducMind`, public, tạo 2026-08-19, push gần nhất 2026-10-18, 0 star/fork, không có issue/PR nào |
| Nhánh | chỉ `main` (repo local có thêm nhánh phiên làm việc của Arena) |
| Lịch sử | **1 commit duy nhất** (`cb12a22`, squash toàn bộ 187 file) → không có lịch sử để review/blame |
| Ngôn ngữ | 325 file Java (~22.100 LOC main + ~3.360 LOC test), 12 file SQL, 12 file MD |
| Backend | Java 21, Spring Boot **4.1.1** (dùng tên starter mới `spring-boot-starter-webmvc`, `org.springframework.boot.persistence.autoconfigure.EntityScan`), Spring Data JPA, Spring Security + JJWT 0.12.6, Flyway, PostgreSQL 17 + pgvector, Redis |
| Frontend | **0 file code** — chỉ có `frontend/src/**/.gitkeep` |
| AI worker (Python) | **0 file code** — chỉ có 4 thư mục `.gitkeep` |
| Migrations | 11 file `V1..V11` trong `backend/educmind-app/src/main/resources/db/migration` (+ 1 bản copy chết ở `database/migrations/`) |
| Endpoint | 104 annotation mapping (103 ở module + `HealthController`), 27 controller |
| Tài liệu | 6 file trong `docs/` (~825 dòng), viết theo kiểu "living specification" (bảng yêu cầu, API catalog, TBD tracker) |

### Bản đồ module (LOC / controller / test)

| Module | Java | LOC | Controller | Endpoint | Test |
|---|---:|---:|---:|---:|---:|
| module-auth | 38 | 2.497 | 3 | 12 | **0** |
| module-schoolclass | 15 | 1.296 | 1 | 7 | **0** |
| module-course | 41 | 4.016 | 4 | 28 | **0** |
| module-assignment | 13 | 1.273 | 1 | 9 | **0** |
| module-assessment | 36 | 3.148 | 2 | 13 | 1 |
| module-analytics | 39 | 2.771 | 5 | 11 | **0** |
| module-rag | 40 | 3.088 | 3 | 3 | 10 |
| module-teacher-assistant | 26 | 2.586 | 2 | 6 | 7 |
| module-notification | 22 | 1.757 | 1 | 7 | 4 |
| module-reports | 26 | 1.971 | 4 | 7 | 6 |
| module-tutor | 0 | 0 | 0 | 0 | 0 |
| module-personalization | 0 | 0 | 0 | 0 | 0 |
| educmind-common | 26 | 959 | — | — | **0** |

→ Code thực tế phủ **8/10 module nghiệp vụ**; `module-tutor` (M8) và `module-personalization` (M9) mới là "vỏ" (pom + thư mục rỗng), và `educmind-app` **không** khai báo dependency tới 2 module này.

---

## 3. Điểm mạnh (nên giữ và phát huy)

1. **Ranh giới module được tôn trọng bằng cổng (port)**: giao tiếp chéo module đi qua `educmind-common/port/*` (`CourseAccessPort`, `ClassAccessPort`, `UserLookupPort`, `LearningEvidencePort`, `RetrievalPort`, `AiUsagePort`…). Đây là cách đúng để giữ modular monolith không bị "rối chỉ".
2. **Envelope thống nhất + xử lý lỗi tập trung**: `ApiResponse<T>` + `GlobalExceptionHandler` bắt riêng 405/404/403/400 để không bị nuốt thành 500 (kèm comment giải thích nguyên nhân thực tế — dấu hiệu dev đã test bằng tay).
3. **Auth làm khá kỹ**: refresh token lưu **SHA-256 hash** (không lưu plaintext), rotation + revoke khi refresh, revoke toàn bộ khi đổi/reset mật khẩu, không tiết lộ user-enumeration ở luồng reset password, BCrypt, `must_change_password` chặn toàn cục qua filter.
4. **Phần RAG là điểm sáng kỹ thuật**: chunking theo ký tự có overlap, `DocumentTextExtractor` (PDFBox), batch embedding có retry theo error code (`AI_RATE_LIMITED/AI_TIMEOUT/AI_UNAVAILABLE`), ghi chunk bằng `JdbcTemplate` + `?::vector` (tránh phải map kiểu pgvector vào Hibernate), **hybrid retrieval** = vector (HNSW cosine) + keyword `to_tsvector('simple')` + fusion RRF; `KeywordQueryBuilder` chỉ giữ `[\p{L}\p{N}]` nên **không thể chèn toán tử tsquery** (chống injection đúng cách). Migration V11 có index HNSW + GIN, `UNIQUE(resource_id, chunk_index)`.
5. **AI có kỷ luật vận hành**: `StuckRequestCleaner` (job định kỳ đánh FAILED request kẹt PROCESSING, điều kiện status nằm trong câu UPDATE nên không ghi đè request vừa xong), quota 28 lượt/24h/GV, `ai_observability_logs`, review gate cho output AI (`GenerationReviewService`).
6. **Mastery là công thức tất định, tách khỏi Spring** (`MasteryService.computeScore`) → đúng nguyên tắc "AI không quyết định điểm"; `REQUIRES_NEW` để lỗi tính mastery không làm hỏng luồng nộp/chấm bài.
7. **Tài liệu có tư duy**: `docs/` phân tầng Requirements → Architecture → DB → API → AI/RAG → Coding rules, có đánh dấu `[DECIDED]`/`[TBD]`/P0–P3. Đây là thứ hiếm thấy ở đồ án.

---

## 4. Vấn đề chặn (P0)

### 4.1. Không thể "boot" hệ thống từ DB trắng 🔴

- Không có file seed nào (`database/seed/` chỉ có `.gitkeep`), không có `CommandLineRunner`/`ApplicationRunner`, không có Flyway `afterMigrate` callback.
- Không có API tạo `school`: `docs/API.md` ghi `POST/GET /api/v1/schools` (SUPER_ADMIN) nhưng **không tồn tại `SchoolController`** (module-schoolclass chỉ có `ClassController`).
- Không có đường tạo `SUPER_ADMIN` đầu tiên: `UserService.createUser` chỉ cho `SCHOOL_ADMIN` gọi, còn `register` cần invite code do TEACHER tạo → con gà–quả trứng.

**Hệ quả:** clone repo, chạy `docker compose up`, `mvnw spring-boot:run` → app lên nhưng **không có bất kỳ tài khoản nào để đăng nhập**. README không đề cập bước này.

**Đề xuất:** thêm `database/seed/V900__dev_seed.sql` (idempotent, chỉ bật ở profile `dev`) hoặc một `DevBootstrapRunner` đọc `educmind.bootstrap.*` để tạo school + SUPER_ADMIN + SCHOOL_ADMIN + TEACHER/STUDENT mẫu; tài liệu hoá trong README.

### 4.2. Leo thang đặc quyền + mất `school_id` ở `POST /api/v1/users` 🔴

`backend/module-auth/.../service/UserService.java:50-72`:

```java
User user = new User(request.getEmail(), passwordHash, request.getFullName(), request.getRole()); // :62
user.setMustChangePassword(true);
user = userRepository.save(user);                       // school_id KHÔNG được set
if (request.getClassId() != null) { classAccessPort.addMember(...); }  // không kiểm tra lớp thuộc trường nào
```

Ba lỗi cùng một chỗ:

1. **Role không bị giới hạn**: `CreateUserRequest.role` là enum tự do; `@PreAuthorize("hasRole('SCHOOL_ADMIN')")` chỉ chặn *ai gọi*, không chặn *tạo ra cái gì*. Một SCHOOL_ADMIN (hoặc ai chiếm được token đó) có thể tạo **`SUPER_ADMIN`**, nhận `temporaryPassword` trong response (`:71`) rồi đăng nhập → **toàn quyền hệ thống**. Trái với `docs/REQUIREMENTS.md` ("chỉ cấp tài khoản GV/HS").
2. **`school_id` không bao giờ được gán** cho user do admin tạo (`setSchoolId` chỉ xuất hiện trong `AuthService.register` khi có invite code). Hệ quả: `GET /api/v1/schools/{id}/analytics/overview` (kiểm tra `schoolId.equals(actor.get().schoolId())`) và mọi báo cáo theo trường **luôn 403/ rỗng** với user tạo qua kênh chính.
3. **Không scope theo trường**: `classId` do client gửi, không kiểm tra lớp có thuộc trường của admin hay không → SCHOOL_ADMIN trường A gán được học sinh vào lớp trường B.

**Đề xuất (ngắn):**
```java
if (request.getRole() != Role.TEACHER && request.getRole() != Role.STUDENT) throw FORBIDDEN;
user.setSchoolId(actor.getSchoolId());
classAccessPort.requireClassInSchool(request.getClassId(), actor.getSchoolId());
```
+ thêm test cho ma trận role này.

### 4.3. Tài khoản DISABLED vẫn dùng được hệ thống 🔴

- `AuthService.login` (`:73-95`) chỉ so `passwordEncoder.matches(...)`, **không** kiểm tra `user.getStatus()`.
- `JwtAuthenticationFilter` (`:80`) đọc user chỉ để check `isMustChangePassword()`, **không** check `DISABLED`; token cũ vẫn sống tới 15 phút (và refresh token tới 7 ngày vẫn đổi được access token mới vì `refresh` cũng không check status).
- `UserStatus.DISABLED` tồn tại trong entity nhưng chỉ được dùng ở `UserLookupPortImpl` — tức là một số service *có* check, một số *không* → hành vi không nhất quán.

**Đề xuất:** chặn ở cả 3 chỗ (login, filter, refresh) + revoke all refresh tokens khi set DISABLED.

### 4.4. Bí mật bị commit vào repo 🔴/🟠

`backend/educmind-app/src/main/resources/application.properties`:

```
:5   spring.datasource.password=educmind_dev
:17  educmind.jwt.secret=0f2c9890403a90970ab1efee4955af1548f29d0f267ed86420e300068607a546   (64 hex = 512-bit, dùng làm HMAC key)
```

`.gitignore` đã ignore `.env` nhưng không giúp gì khi secret nằm thẳng trong file tracked. JWT secret bị lộ = ai có repo **ký được token hợp lệ cho bất kỳ user/role nào** (đã có `sub`, `role`, `school_id` claim).

**Đề xuất:** `educmind.jwt.secret=${JWT_SECRET}` (bắt buộc, fail-fast nếu thiếu), `spring.datasource.password=${DB_PASSWORD:educmind_dev}`; tách `application-dev.properties` / `application-prod.properties`; nếu secret đã từng public → coi như đã lộ, sinh lại.

---

## 5. Vấn đề nghiêm trọng (P1)

| Vấn đề | Bằng chứng | Ảnh hưởng |
|---|---|---|
| **Không có rate limit / lockout ở login** | `AuthService.login` không đếm attempt; không có bucket4j/Redis counter nào cho auth (Redis có trong compose nhưng chưa dùng cho auth) | Brute-force mật khẩu; BCrypt chỉ làm chậm chứ không chặn |
| **Chính sách mật khẩu không áp dụng khi đăng ký** | `RegisterRequest.password` chỉ có `@NotBlank`; `PasswordResetConfirmRequest` mới có `@Size(min=8)` | Mật khẩu "1" hợp lệ khi self-register bằng invite code |
| **`@Valid` chỉ dùng 30 lần / 72 DTO**; nhiều endpoint nhận `Map<String,Object>` thô | `ReportExportController.create(@RequestBody Map<String, Object> body)` | Dữ liệu rác vào tầng nghiệp vụ, lỗi 500 thay vì 422 |
| **`module-reports` import trực tiếp repository/entity/service của 3 module khác** | `module-reports/pom.xml` depends `module-assessment`, `module-assignment`, `module-analytics`; `ClassProgressReportService` import `AssignmentRepository`, `AssignmentSubmissionRepository`, `TeacherMasteryOverviewQueryService`… | Vi phạm chính `docs/CODING_RULES.md §1` ("cấm import Service/Repository của module khác"). Reports hiện là module "biết tuốt", coupling cao nhất repo |
| **Không có CI** | không có `.github/` | README hứa "CI/CD via GitHub Actions" nhưng 0 workflow; không ai biết code có build được không |
| **Không có test cho phần rủi ro nhất** | 0 test ở `module-auth`, `module-schoolclass`, `module-course`, `module-assignment`, **`module-analytics`**; 5/13 module thậm chí không khai báo `spring-boot-starter-test` trong pom | Mastery/gap/recommendation là công thức nhiều ngưỡng (half-life, gap 0.50/0.65) mà không có một assert nào |
| **Không có integration/e2e test** | `tests/{unit,integration,e2e}` trống (chỉ `.gitkeep`); README hứa JUnit 5 + **Testcontainers** nhưng repo không có dependency Testcontainers | Luồng "upload → RAG → mastery → notification" chưa từng được chạy tự động |
| **CSV export không chống formula injection** | `CsvReportWriter.escape()` chỉ xử lý `, " \n \r` | Ô bắt đầu bằng `= + - @` sẽ được Excel thực thi; dữ liệu nguồn có `full_name` do người dùng nhập |
| **`spring.jpa.show-sql=true`** (:11) | log mọi câu SQL, kể cả tham số khi bind trace | Ồn + rò rỉ dữ liệu trong log production |
| **Job dọn request kẹt dựa trên `createdAt`** | `AiGenerationRequestRepository.failStuck` dùng `r.createdAt < :cutoff` | Nếu request xếp hàng chờ lâu (queue) sẽ bị đánh FAILED oan; nên dùng `startedAt`/`updatedAt` |

---

## 6. Tài liệu vs Code: đã lệch (P1 — nhưng sửa rẻ)

| Chủ đề | `docs/` + README nói | Code thực tế |
|---|---|---|
| AI Worker | Service Python riêng ở `backend/ai-worker/` (parse/chunk/embed/retrieval/queue_consumer/prompt_registry) | **Không có dòng Python nào**. Toàn bộ pipeline nằm trong Java `module-rag` (`pipeline/`, `retrieval/`, `store/`), chạy in-process qua `@Async`, không có queue, không có REST nội bộ |
| Giao tiếp liên module với RAG | "Ngoại lệ REST duy nhất: `POST /api/v1/internal/retrieval/search`" (`ARCHITECTURE.md`, `API.md`, `CODING_RULES.md`) | Endpoint đó **không tồn tại**; dùng `RetrievalPort` in-process |
| Thứ tự migration | `ARCHITECTURE.md §7`: V4 = ai-worker, V5 analytics, V6 teacher-assistant, V7 tutor, V8 personalization, V9 notification+reports, V10 observability | V1 auth+schoolclass, V2 course, V3 course_subject_and_members, V4 assignment+assessment, V5 analytics LO fix, V6 assignment_max_score, V7 analytics tables, V8 notification, V9 reports_export, V10 teacher_assistant, **V11 rag** |
| Chiều vector | `DATABASE.md`: `VECTOR(?)` P0 chưa chốt, placeholder `VECTOR(1536)` | `V11__module_rag.sql`: `VECTOR(768)` + `gemini-embedding-001` (đã chốt rồi, docs chưa cập nhật) |
| Package dùng chung | `common.envelope`, `common.pagination` (`OffsetPageUtil`, `CursorMessageUtil`, `CursorSequenceUtil`), `common.authorization.CourseAuthorizationService`, `common.events`, `common.aigateway`, `common.review` | Thực tế: `common.envelope`, `common.event` (**số ít**), `common.exception`, **`common.port`** (thay cho aigateway/authorization), `common.pagination.PaginationSupport` (**chỉ 1 util offset**; cursor message/sequence được viết inline trong module) — không có `common.review` |
| Endpoint catalog | có `POST/GET /api/v1/schools` | **Không có** `SchoolController` |
| Endpoint catalog | gắn/gỡ lớp cho course: `POST/DELETE /api/v1/courses/{id}/classes` | Không có; thay bằng `POST /courses/{id}/members/students`, `/members/teachers`, `DELETE /courses/{id}/members/{userId}`, `GET /courses/{id}/members` (không có trong API.md) |
| Ngoài danh mục | — | `SubjectController` (`/api/v1/subjects`), `RagBackfillController` (`/api/v1/admin/rag/backfill`), notification preferences, `/api/v1/schools/{id}/reports/ai-usage` — đều không có trong `API.md` |
| Java version | "Java 17+" | `pom.xml` `<java.version>21</java.version>` |
| Frontend | React (TS), `npm run dev`, Jest | `frontend/` rỗng (chỉ `README.md` 0 byte + `.gitkeep`) |
| Module count | README mô tả 11 module | 13 module Maven, trong đó 2 rỗng |

**Đề xuất:** chọn một nguồn sự thật (docs), rồi cập nhật docs theo code — hoặc ngược lại nếu kế hoạch vẫn là tách Python worker. Đừng để nguyên trạng thái "docs nói A, code làm B" vì người chấm/reviewer sẽ đối chiếu.

---

## 7. README: hướng dẫn chạy hiện không chạy được

```
docker compose up -d db redis        # ❌ service tên là "postgres", không phải "db" → lỗi "no such service"
cd database && flyway migrate         # ❌ không có flyway.conf/CLI; migration thật nằm trong educmind-app và do Spring Flyway chạy
cd backend && ./mvnw spring-boot:run  # ❌ packaging=pom, phải là ./mvnw -pl educmind-app spring-boot:run (hoặc cd educmind-app)
cd backend/ai-worker && python main.py# ❌ thư mục rỗng
cd frontend && npm install            # ❌ không có package.json
```

Ngoài ra: `backend/README.md`, `frontend/README.md`, `tests/README.md`, `database/README.md` **đều 0 byte** (file rỗng được commit); không có `.env.example` dù README nói "see .env.example"; không có `LICENSE`.

**Đề xuất:** viết lại mục "How to Run" theo đúng repo (compose `postgres`+`redis` → `./mvnw -pl educmind-app spring-boot:run` → seed data), thêm `Makefile` hoặc `scripts/dev-up.sh` (đã có `backend/scripts/` — kiểm tra và dùng nó).

---

## 8. Chất lượng test & CI

- 28 file test / ~3.360 LOC cho ~22.100 LOC main → tỉ lệ thấp, và **phân bố lệch**: chỉ những module làm sau cùng (rag, reports, teacher-assistant, notification) có test.
- Toàn bộ là **unit test thuần Mockito** (`mock(...)`, `ArgumentCaptor`), không có `@SpringBootTest`, không Testcontainers → **chưa có bằng chứng nào là app khởi động được**, JPA mapping/JPQL/Flyway khớp schema.
- Có 2 test kiểu "live" bị vô hiệu hoá bằng `@EnabledIfEnvironmentVariable` (`GeminiEmbeddingProviderLiveTest`, `GeminiProviderLiveTest`) — cách làm tốt.
- Tên test tiếng Việt không dấu (`macDinh10Phut_truyenDungMocThoiGianVaThongBao`) → đọc được, nên thống nhất quy ước.

**Đề xuất thứ tự ưu tiên test:** (1) `AuthService` (login/refresh/rotation/reset), (2) `UserService.createUser` (ma trận role + school scope), (3) `MasteryService.computeScore` + `KnowledgeGapService.evaluate` (thuần hàm, dễ test nhất, giá trị cao nhất), (4) 1 smoke test `@SpringBootTest` + Testcontainers chạy Flyway V1→V11 (bắt lỗi migration ngay).

---

## 9. Vệ sinh repo (P2, sửa trong 30 phút)

| Việc | Chi tiết |
|---|---|
| File migration trùng | `database/migrations/V1__module_auth_schoolclass.sql` **giống hệt** file trong `educmind-app` (diff rỗng) → 2 nguồn sự thật, dễ sửa nhầm bản chết |
| Module rỗng vẫn build | `module-tutor`, `module-personalization` có pom nhưng không có code; `educmind-app` không depend 2 module này → nên tạm bỏ khỏi `<modules>` hoặc ghi rõ "scaffold" |
| 51 file `.gitkeep` | phần lớn nằm ở `frontend/src/modules/**` và `ai-worker/**` — phản ánh kế hoạch, không phải code; cân nhắc gộp thành 1 `PLANNED.md` |
| Lịch sử git | 1 commit squash 187 file → mất khả năng review/blame. Nên commit theo module từ đây |
| `.gitattributes`/`HELP.md` | `backend/HELP.md` (30 dòng) là file mặc định của Spring Initializr, nên xoá |
| README rỗng | 4 file README 0 byte → xoá hoặc viết nội dung |
| Comment tiếng Việt không dấu rải rác | (`"Sai email hoac mat khau"`) trong khi phần lớn comment khác có dấu → thống nhất |

---

## 10. Điểm cộng về thiết kế (đáng giữ)

- **Nguyên tắc "AI không quyết định điểm"** được thực thi thật: mastery/gap là công thức trong `MasteryService`/`KnowledgeGapService`, AI chỉ sinh nháp và luôn qua `GenerationReviewService`.
- **3 chuẩn pagination** được phân biệt rõ và áp dụng đúng chỗ (offset cho danh sách, `since_seq` cho notification, cursor message dành cho Tutor — chưa implement).
- **DB design kỹ**: soft-delete, UUID PK, `TIMESTAMPTZ`, CHECK constraint cho status, index cho FK, `UNIQUE` chống trùng, `learning_objective` khoá theo `(lesson_id, code)` đúng như cảnh báo trong REQUIREMENTS.
- **Các quyết định có ghi lý do** ngay trong code (`[DECIDED - Nghiep xac nhan]`, "đã từng xảy ra thực tế: endpoint ... trả 500 thay vì 404").

---

## 11. Lộ trình đề xuất (2 tuần, theo thứ tự)

**Tuần 1 — "chạy được + an toàn"**
1. Bỏ secret khỏi repo → `${JWT_SECRET}` biến môi trường; tách profile `dev/prod`; tắt `show-sql` ở prod. *(0.5 ngày)*
2. Sửa `createUser`: giới hạn role ∈ {TEACHER, STUDENT}, gán `school_id` từ actor, kiểm tra class cùng trường. *(0.5 ngày + test)*
3. Chặn `DISABLED` ở login / filter / refresh. *(0.5 ngày)*
4. Seed/bootstrap: school + SUPER_ADMIN + dữ liệu demo + viết lại "How to Run". *(1 ngày)*
5. Thêm `@Size(min=8)` cho mật khẩu ở register; rate-limit login bằng Redis (đã có sẵn hạ tầng). *(0.5 ngày)*

**Tuần 2 — "tin được + nhất quán"**
6. CI: `.github/workflows/ci.yml` (JDK 21, `./mvnw -B verify`, cache `~/.m2`) + smoke test Testcontainers chạy Flyway. *(1 ngày)*
7. Test cho `MasteryService`/`KnowledgeGapService` + `AuthService` (ma trận quyền). *(2 ngày)*
8. Đồng bộ docs: chốt "RAG nằm trong Java module-rag" hay "tách Python worker", rồi sửa `README/ARCHITECTURE/DATABASE/API/CODING_RULES` (bảng §6 ở trên là checklist). *(1 ngày)*
9. Dọn vệ sinh repo (§9). *(0.5 ngày)*

**Sau đó (nếu còn thời gian):** `module-tutor` (M8) → `module-personalization` (M9) → frontend, mỗi module kèm 1 endpoint e2e test.

---

## 12. Câu hỏi mở tôi cần bạn xác nhận

1. **RAG**: giữ trong Java (`module-rag`) — nghĩa là bỏ kế hoạch Python worker — hay vẫn tách worker Python như docs? (Ảnh hưởng toàn bộ §6.)
2. **`module-reports` được phép import chéo module** như hiện tại (vì nó là module tổng hợp), hay phải refactor sang port? Nếu cho phép, hãy ghi ngoại lệ vào `CODING_RULES.md` để reviewer không reject.
3. Có cần tôi **triển khai luôn** các fix P0 ở §4 (bắt đầu từ secret + createUser + DISABLED + seed) trên nhánh `arena/e8763d6d-educmind` không?
