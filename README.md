# AI-Powered Employee Leave Management System

An interview-friendly Java full-stack HR leave system. It provides JWT-protected employee, manager, and HR flows; auditable leave approval; operational dashboards; and a pluggable local AI assistant boundary.

## Delivered vertical slice

- Login / current-user session and role-based UI navigation.
- Admin configuration for departments, leave types, holidays, employee creation and employee-manager assignment.
- Department and employee administration APIs, leave balances, holiday-aware working-day calculations, conflict checks, approval/rejection/cancellation, audit records and notifications.
- MySQL schema and sample seed, Swagger, Docker Compose, CI, unit tests, and a functional React dashboard/apply/history/approval interface.
- AI policy assistant supports admin PDF upload, PDF extraction/chunking, Ollama embeddings, reliable ChromaDB indexing/query, cited per-user chat history, authenticated balance/history/holiday tools, confirmation-gated leave submission, and admin policy reindexing. Ollama and ChromaDB must be running for live AI answers; AI cannot approve or reject leave.

## Run locally

1. Copy `.env.example` to `.env`, set the required values, and start MySQL with `docker compose up -d mysql`. Docker Compose reads `.env`; for a direct Spring Boot launch, provide the same variables through the shell or IDE because Spring Boot does not load `.env` automatically. The application uses only the `aiemployeeleavemanagementsystem` database. Do not configure it to use or alter `employeeleavemanagementsystem`.
2. `cd backend; mvn spring-boot:run`.
3. `cd frontend; npm install; npm run dev`.
4. Open `http://127.0.0.1:5175`; Swagger is at `http://127.0.0.1:8080/swagger-ui/index.html`.

The first admin comes from `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD`; do not retain its example values outside local development.

For Docker Compose, copy `.env.example` to `.env`, provide the required database and JWT values, then run `docker compose up --build`. Docker exposes the frontend on `5175`, backend on `8080`, MySQL on `3306`, ChromaDB on host port `8001` (container port `8000`), and Ollama on `11434`. The Docker backend connects to ChromaDB as `http://chroma:8000`; host-local development uses the verified ChromaDB 1.5.9 setup at `http://127.0.0.1:8001`. Docker's ChromaDB 0.6.3 RAG compatibility with this backend is unverified.

## Architecture

React -> Axios/JWT -> Spring REST services -> MySQL. Spring AI is an adapter boundary configured via environment, while leave policy and authorization stay in Java services. ChromaDB is reserved for uploaded policy chunks, not HR data.

## Testing

`mvn test` runs backend unit tests. `npm test` runs frontend tests. CI builds/tests both without OAuth or live model credentials.

The frontend production bundle is built with `npm run build`. Browser verification requires a running backend and legitimate, authorized accounts for each role; do not reset passwords or create records in an existing environment merely to test it.

## Core HRMS APIs

- `GET/POST/PUT/DELETE /api/departments` — HR department administration; deletion is refused while employees are assigned.
- `GET/POST/PUT /api/employees` — admin-managed employee/manager assignment; profile retrieval enforces ownership and direct-report access.
- `GET/POST/PUT /api/leave-types` and `GET/PUT /api/leave-balances` — leave configuration and admin-only entitlement upsert. An upsert locks the employee/leave-type balance, applies only the required delta, and writes an immutable adjustment transaction with an admin-supplied reason.
- `POST /api/leaves`, `PUT /api/leaves/{id}/approve|reject|cancel` — workflow routes with manager/team authorization and balance accounting.
- `POST /api/ai/policies` and `POST /api/ai/policies/{id}/reindex` — admin-only PDF ingestion and idempotent policy-index repair; `POST /api/ai/chat` and `/api/ai/conversations/**` — authenticated cited policy chat and private history.
- `GET /api/leave-balances/history`, `GET /api/notifications`, `GET /api/notifications/unread-count`, `PUT /api/notifications/{id}/read`, and paginated `GET /api/audit-logs`.

Swagger is available at `/swagger-ui.html`. API error bodies use a consistent `timestamp`, `status`, `error`, `message`, and `fields` contract.

## DTO and security strategy

The API DTO catalog lives under `common/dto`; new notification, department, audit, and balance endpoints return DTOs instead of JPA objects. Entitlements, department administration, and audit retrieval are admin-only. Employee-owned routes derive the employee from the JWT principal; manager routes check direct-manager assignment. Password hashes remain JSON-hidden in the entity, and response DTOs never carry password fields.

## Database verification status

The service uses only the `aiemployeeleavemanagementsystem` database. Its JDBC default includes `createDatabaseIfNotExist=true` for local users with database-create permission; Docker Compose also provisions this exact name, and `database/schema.sql` creates its tables there. The existing `employeeleavemanagementsystem` database is intentionally never read, modified, overwritten, or deleted. The service uses JPA transactional boundaries, a pessimistic balance-row lock, optimistic version columns, and immutable per-leave accounting entries: one approval debit and, where eligible, one cancellation credit. Unit tests cover duplicate debit prevention, insufficient-balance behavior, and cancellation restoration.

When validating an existing deployment, supply a restricted database account, keep Hibernate on `validate`, disable Flyway unless a reviewed migration is explicitly approved, and disable seed administration. Take and verify a recoverable backup before any workflow that writes test data. Never run `database/schema.sql` against an existing deployment.

## OAuth and AI setup

Set Google credentials and configure the callback `/login/oauth2/code/google` in Google Cloud before testing OAuth. Start Ollama, pull `llama3.2` and `nomic-embed-text`, then configure the supplied environment variables. These external integrations are not verified in this workspace.

For AI policy ingestion, start ChromaDB at `CHROMA_URL` (default `http://127.0.0.1:8001`) and upload PDF documents from the admin Policy documents page. `POLICY_MAX_FILE_SIZE` and `POLICY_MAX_REQUEST_SIZE` control multipart limits. The backend keeps policy chunks and chat history in MySQL while ChromaDB stores vectors; conversation access is restricted to the authenticated user.
