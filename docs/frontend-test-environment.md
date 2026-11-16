# Isolated frontend-test environment

This profile is for authenticated browser verification only. It must never point at the operational `aiemployeeleavemanagementsystem` schema or the unrelated `employeeleavemanagementsystem` schema.

## Stage 1 configuration

Use `SPRING_PROFILES_ACTIVE=frontend-test` with these test-only environment variables stored outside source control:

- `FRONTEND_TEST_DB_URL` and `FRONTEND_TEST_FLYWAY_DB_URL`: both must target `aiemployeeleavemanagementsystem_frontend_test` without `createDatabaseIfNotExist=true`.
- `FRONTEND_TEST_DB_USER`, `FRONTEND_TEST_DB_PASSWORD`, `FRONTEND_TEST_FLYWAY_DB_USER`, and `FRONTEND_TEST_FLYWAY_DB_PASSWORD`: test-schema-only credentials.
- `FRONTEND_TEST_JWT_SECRET`: a unique test-only secret.
- `FRONTEND_TEST_FLYWAY_ENABLED`: remains `false` until explicit approval to migrate the isolated schema.

`application-frontend-test.yml` binds the backend to port 8081, allows only `http://127.0.0.1:5175`, uses ChromaDB only at `http://127.0.0.1:8001`, and disables automatic admin seeding. `FrontendTestEnvironmentGuard` rejects missing values, either protected schema, an unverifiable schema, any non-test schema, and URLs that request automatic database creation before datasource or Flyway auto-configuration.

Run the frontend in Vite's `frontend-test` mode. Copy `frontend/.env.frontend-test.example` to the ignored `frontend/.env.frontend-test` only when the isolated backend is available.

## Future approved account setup

After the isolated schema has been explicitly created and migrated, establish only these test-only roles there:

| Role | Minimum supporting records |
| --- | --- |
| ADMIN | One department, active leave types, holidays, audit-visible sample data. |
| MANAGER | A department and at least one direct-report employee; optional pending leave record for read-only approval-list coverage. |
| EMPLOYEE | Department, manager, leave balances, balance ledger entries, and optional historical leave records for tables/pagination. |

Passwords must be generated and supplied at setup time through a secret store or local ignored environment file; they must not be committed, logged, or reused from operational/demo configuration. No policy document, Chroma collection, embedding, or AI conversation is required to open and inspect the assistant UI.

## Isolation rules

- The existing ChromaDB directory `D:\PythonAI\chroma_db` and port 8000 are out of scope.
- A separate ChromaDB instance on port 8001 must use a separate empty directory/volume.
- Ollama on port 11434 may be reused for service availability only; browser verification must not send prompts or create embeddings.
- Never mount or run `database/schema.sql` for this profile because it hard-codes the operational schema name.
