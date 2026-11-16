# AI-Powered Employee Leave Management System

## Overview
Portfolio-ready HR leave workflow with React/Vite frontend, Spring Boot 3/Java 21 backend, MySQL source of truth, and an optional local Spring AI/Ollama policy assistant.

## Stack and architecture
- React, React Router, Bootstrap 5, Context API, Axios, Vitest/RTL.
- Spring Boot, Security/JWT/OAuth2 Client, JPA, Validation, Lombok, Mail, springdoc, Spring AI.
- MySQL 8 stores operational data; ChromaDB only stores policy embeddings; Ollama provides local chat/embeddings.
- Layering: controllers -> DTOs/services -> repositories/entities. Constructor injection only.

## Conventions
- REST uses `/api`, DTOs only, RFC-style validation errors, and UTC timestamps.
- Secure endpoints derive identity and role exclusively from the authenticated principal.
- Roles are `EMPLOYEE`, `MANAGER`, `ADMIN`; no public admin registration.
- Never expose passwords, JWTs, secrets, entities, or private employee information.
- Leave approval is transactional, locked, audited, and idempotent. AI may assist but cannot decide approval.
- React calls the backend through `VITE_API_BASE_URL`; no fabricated dashboard data.
- Add JUnit/Mockito or Vitest coverage for completed behavior; do not commit secrets.

## Current milestone
Initial vertical slice: project scaffold, JWT auth, departments/employees, leave workflow, dashboards, database schema, Docker/CI, and React role-aware UI.
