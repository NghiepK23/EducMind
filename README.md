# Software-Technology-Team3

## Introduction

**EducMind** is an AI-integrated Learning Management System (LMS) designed for a single school (MVP scope). It supports three main actors — **School Admin**, **Teacher**, and **Student** — and combines traditional LMS features (course/lesson/resource management, assignments, assessments) with AI-powered capabilities built on top of a Retrieval-Augmented Generation (RAG) foundation:

- **AI Teacher Assistant** — helps teachers draft lesson plans, question banks, and assessment matrices.
- **AI Tutor** — provides scaffolded, context-aware conversational support to students (hints → guiding questions → concept explanation → worked examples).
- **Personalization Engine** — turns mastery/knowledge-gap data into AI-explained recommendations and personalized learning paths.

A core design principle across every AI feature is **human-in-the-loop**: no AI-generated content (lesson content, questions, recommendations, insight reports) is ever applied to the system without teacher review and approval. Mastery scores and knowledge gaps are always computed by a deterministic rule engine, never decided by the AI.

The backend follows a **Modular Monolith** architecture (Spring Boot) with a dedicated **AI Worker** service (Python) handling the RAG pipeline (parsing, chunking, embedding, retrieval), backed by PostgreSQL/pgvector and Redis.

## Team Members
- Hoih Nghiep
- Nguyen Ba Thien
- Mai Thi Thanh Tu
- Ta Cong Hoang
- Pham Viet Anh

## Technologies

**Backend**
- Java 17+, Spring Boot (Modular Monolith)
- Spring Data JPA / Hibernate
- Spring Security + JWT (access/refresh token)
- Flyway (database migration)

**AI / RAG Worker**
- Python
- RAG pipeline: parsing → semantic chunking → embedding → hybrid retrieval (vector + keyword) → fusion/rerank
- AI Gateway pattern — pluggable providers: Gemini / OpenAI / LocalLLM

**Database & Infrastructure**
- PostgreSQL + `pgvector` extension (embedding storage)
- Redis (cache, and interim queue for async processing)
- Docker & Docker Compose (local dev), CI/CD via GitHub Actions

**Frontend**
- React (TypeScript)
- REST client with a shared response envelope (`{ success, data, error, meta }`)

**Testing**
- JUnit 5 + Testcontainers (backend unit/integration)
- Jest (frontend)

## Project Structure

```
EducMind/
├── README.md
├── docs/                          # Living specification — read in this order
│   ├── REQUIREMENTS.md            # Business requirements, actors, ownership model
│   ├── ARCHITECTURE.md            # System architecture, module dependency map
│   ├── DATABASE.md                # Consolidated schema, migration order
│   ├── API.md                     # Endpoint catalog, pagination standards
│   ├── AI_RAG.md                  # RAG pipeline & AI modules (Teacher Assistant/Tutor/Personalization)
│   └── CODING_RULES.md            # Shared coding conventions for all squads
│
├── frontend/
│   └── src/
│       ├── modules/                # auth, school-class, course, assignment, assessment,
│       │                            # analytics, teacher-assistant, tutor, personalization,
│       │                            # notification, reports
│       ├── shared/                 # API client, pagination hooks, route guards
│       └── assets/
│
├── backend/
│   ├── educmind-common/           # Shared building blocks — build this first
│   │   └── src/main/java/com/educmind/common/
│   │       ├── envelope/           # ApiResponse, GlobalExceptionHandler
│   │       ├── pagination/         # Offset / cursor-message / cursor-sequence utils
│   │       ├── authorization/      # Ownership & co-teacher checks
│   │       ├── events/             # Cross-module application events
│   │       ├── aigateway/          # AIProvider interface, PromptRegistry, quota service
│   │       └── review/             # Human-in-the-loop review state machine
│   │
│   ├── module-auth/                # Authentication, invite codes, tokens
│   ├── module-schoolclass/         # School / Class / Class members
│   ├── module-course/              # Course / Lesson / Resource
│   ├── module-assignment/          # Assignments & submissions
│   ├── module-assessment/          # Question bank & assessments
│   ├── module-analytics/           # Learning evidence, mastery, knowledge gap, recommendation
│   ├── module-teacher-assistant/   # AI-generated lesson plans / questions / matrices
│   ├── module-tutor/               # AI Tutor conversations
│   ├── module-personalization/     # Learning path & insight reports
│   ├── module-notification/        # In-app notifications (polling)
│   ├── module-reports/             # Aggregated dashboards & exports
│   └── ai-worker/                  # Python service — RAG pipeline & retrieval
│
├── database/
│   ├── migrations/                 # Flyway scripts, V1 → V10
│   └── seed/                       # Dev & test seed data
│
└── tests/
    ├── unit/
    ├── integration/
    └── e2e/
```

Full architectural rationale, database schema, and API contracts are documented in [`docs/`](./docs).

## How to Run

**Prerequisites**
- Docker & Docker Compose
- JDK 17+ and Maven/Gradle (for local backend development outside Docker)
- Node.js 18+ (for frontend development)
- Python 3.11+ (for `ai-worker` development)

**1. Clone the repository**
```bash
git clone <repo-url>
cd EducMind
```

**2. Start infrastructure (PostgreSQL + pgvector, Redis)**
```bash
docker compose up -d db redis
```

**3. Run database migrations**
```bash
cd database
flyway migrate
```

**4. Run the backend**
```bash
cd backend
./mvnw spring-boot:run
```

**5. Run the AI worker**
```bash
cd backend/ai-worker
pip install -r requirements.txt
python main.py
```

**6. Run the frontend**
```bash
cd frontend
npm install
npm run dev
```

**7. Access the app**
- Frontend: `http://localhost:5173` (or configured port)
- Backend API: `http://localhost:8080/api/v1`

> Environment variables (DB credentials, JWT secret, AI provider API keys) are configured via `.env` — see `.env.example` in each service directory (to be added).

## Git Workflow

- `main`: stable code — always deployable.
- `feature/*`: new features (e.g. `feature/module-auth-login`).
- `fix/*`: bug fixes (e.g. `fix/pagination-cursor-notifications`).

**Rules:**
- Do **not** push directly to `main`.
- All changes go through a Pull Request, reviewed by at least 1 other team member before merging.
- Branch naming should reference the module being worked on when possible (e.g. `feature/module-tutor-scaffolding`), to make review ownership clear across the 11 backend modules.
- Squash or rebase before merging to keep `main` history clean.
