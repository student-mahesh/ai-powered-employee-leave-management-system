# Employee Leave Management Backend

Spring Boot 3 backend for a role-aware leave workflow with an optional local HR-policy assistant. It is deliberately a modular monolith: REST controllers use DTOs, services own business transactions, repositories own persistence, and MySQL is the operational source of truth.

## Stack

- Java 21, Spring Boot 3.4.5, Maven
- Spring Security, BCrypt passwords, stateless JWT bearer authentication
- Spring Data JPA/Hibernate, MySQL 8, Flyway
- springdoc OpenAPI/Swagger
- Optional Spring AI/Ollama (`llama3.2`), `nomic-embed-text`, and ChromaDB for policy RAG

## Architecture and data

`Controller -> request/response DTO -> service -> repository/entity`. Controllers derive the caller from Spring Security; callers never provide an employee identity for employee-owned operations. MySQL stores users, departments, leave types, balances, leave applications, accounting/audit records, policy documents/chunks, conversations, and pending confirmations. ChromaDB stores only policy vectors and maps back to MySQL `policy_chunks.vector_id`.

Flyway migrations are append-only. `V6__allow_leave_cancellation_ledger_entries.sql` changes the old one-entry-per-leave index into a non-unique index so approval debit and eligible-cancellation credit are both preserved. It does not delete or reset data.

## Roles and leave lifecycle

- Employees apply for future dates; weekends and configured holidays are excluded. Requests stay `PENDING` and do not change balances.
- The assigned direct manager or an admin may review. Self-review is prohibited.
- Approval locks the balance row, changes the request to `APPROVED`, and writes one debit ledger entry. Existing accounting prevents a second debit.
- Rejection requires a reason and leaves the balance unchanged.
- An employee may cancel a pending request, or an approved request before its start date. Eligible approved cancellation writes one credit ledger entry.
- Managers can only view/review direct reports; employee routes enforce ownership. Admins retain intended administrative scope.

## AI and RAG

Admins upload PDF policy documents at `POST /api/ai/policies`. The service extracts page text, chunks it, creates embeddings with Ollama, stores vectors in ChromaDB, and stores source/page mappings in MySQL. Policy answers retrieve Chroma vector IDs, resolve MySQL chunks, require lexical evidence, and return `file#page-N` citations; unavailable or inconsistent indexing is reported separately from genuine insufficient evidence. Administrators can repair an existing document with `POST /api/ai/policies/{id}/reindex`.

Balance, history, and holiday lookups derive identity from the authenticated principal. Leave submission is **not** exposed as an LLM tool: deterministic parsing creates a user/conversation-bound pending confirmation, only `YES` submits, `NO` cancels, and repeated `YES` returns the original submitted application. AI cannot approve/reject leave, modify balances, roles, or permissions, or run SQL.

## Prerequisites

- Java 21 and Maven
- MySQL 8, with a database named `aiemployeeleavemanagementsystem`
- Ollama with the `llama3.2` chat model and `nomic-embed-text` embedding model (required for live policy RAG)
- ChromaDB 1.5.9 for the local policy-vector store (required for live policy RAG)

## Configuration

Copy the repository `.env.example` to a local, uncommitted `.env` and set values appropriate for your environment. Docker Compose reads that file; direct Spring Boot launches must receive the variables through the shell or IDE because Spring Boot does not load `.env` automatically. Important variables are:

| Variable | Purpose |
|---|---|
| `DB_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` | Application MySQL connection |
| `MIGRATION_DB_URL`, `MIGRATION_DB_USER`, `MIGRATION_DB_PASSWORD` | Flyway connection; can be a separate least-privilege user |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | JWT signing and expiry |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | Configurable first-admin seed |
| `OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL`, `OLLAMA_EMBEDDING_MODEL`, `CHROMA_URL` | Optional AI/RAG services (`CHROMA_URL` defaults to `http://127.0.0.1:8001`) |
| `APP_URL`, SMTP variables | CORS and optional mail delivery |

No production credentials belong in source control. On startup, the application seeds an admin only when the configured email does not already exist.

## MySQL setup

Create the target database and provision the application and Flyway users according to your local access policy. Use placeholders rather than real credentials:

```sql
CREATE DATABASE aiemployeeleavemanagementsystem;
-- Create/grant <app-user> and <migration-user> only as required by your local setup.
```

Set `DB_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MIGRATION_DB_URL`, `MIGRATION_DB_USER`, and `MIGRATION_DB_PASSWORD` in the local environment before starting Spring Boot. Flyway validates and migrates only `aiemployeeleavemanagementsystem`; never point it at an unrelated database.

## Start locally

Start MySQL by the method appropriate to your machine. For the verified local AI services, use:

Set `CHROMA_PERSISTENT_DIR` to a local, uncommitted Chroma data directory before running the command below.

```powershell
ollama serve
ollama pull llama3.2
ollama pull nomic-embed-text

chroma.exe run --path $env:CHROMA_PERSISTENT_DIR --host 127.0.0.1 --port 8001

cd backend
mvn -B spring-boot:run
```

The Chroma command above uses the local 1.5.9 server and a machine-local persistent directory selected through `CHROMA_PERSISTENT_DIR`; do not commit that directory. It does not use the legacy Docker Chroma service. Swagger is at `http://localhost:8080/swagger-ui/index.html` (also `/swagger-ui.html`).

For Docker Compose, use the repository `.env` with required database and JWT values and run `docker compose up --build` from the repository root. Host ports are frontend `5175`, backend `8080`, MySQL `3306`, ChromaDB `8001`, and Ollama `11434`; inside Docker, the backend uses `CHROMA_URL=http://chroma:8000`. The verified local RAG setup uses ChromaDB 1.5.9 at `http://127.0.0.1:8001`; compatibility of the Docker ChromaDB 0.6.3 image with this backend is unverified.

## Main API usage

Login with `POST /api/auth/login`, use the returned bearer token in Swagger **Authorize**, then use the secured APIs:

- `GET /api/auth/me` — current authenticated user.
- `/api/employees`, `/api/departments`, and `/api/leave-types` — administration (role-restricted).
- `POST /api/leaves`, `GET /api/leaves/my`, and review/cancellation routes — leave workflow.
- `GET /api/dashboard/*` and `/api/reports/*` — role-aware dashboard and reporting routes.
- `POST /api/ai/policies` — admin policy-PDF ingestion; `POST /api/ai/policies/{id}/reindex` — admin idempotent index repair; `POST /api/ai/chat` — authenticated policy chat; `/api/ai/conversations` — caller-owned conversations.

Do not paste real tokens into documentation or tickets.

Example employee creation is admin-only:

```json
{"email":"manager@example.test","fullName":"Example Manager","password":"choose-a-local-test-password","role":"MANAGER","departmentId":null,"managerId":null}
```

## Testing

```powershell
cd backend
mvn -B test
```

The suite covers leave-date and workday rules, insufficient balances, duplicate approval protection, cancellation restoration, direct-manager scope, self-review prevention, AI evidence/citation handling, history/holiday identity binding, conversation ownership, YES/NO confirmation, repeated-YES idempotency, and the absence of an LLM leave-submission tool. These are unit tests using Mockito; they are **not** live MySQL, Flyway, ChromaDB, Ollama, Docker, or Testcontainers integration tests. Run the local stack above for end-to-end verification.

## Known limitations

Email delivery is best-effort when SMTP is not configured. Live RAG requires reachable Ollama and ChromaDB plus an uploaded policy document. OAuth client values may be configured externally but are not required for local JWT-based operation.
