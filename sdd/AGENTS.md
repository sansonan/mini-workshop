# Project Instructions for Codex and AI Agents

These instructions apply when `sdd/` is opened as the project workspace. Read them before working here. More specific instructions may add guidance but may not override approved specifications.

## Project overview and technology stack

The Mini Workshop Booking System lets visitors browse and register for workshops, Admins manage workshops, and Instructors view their assigned workshops and participants. This is a beginner-oriented Spec-Driven Development (SDD) project.

- Backend: Java 21, Spring Boot, Maven
- Frontend: React
- Database: PostgreSQL (`workshop_booking_db`)
- API: JSON REST, versioned under `/api/v1`
- Authentication: Spring Security server-side sessions and secure cookies
- Email: SMTP delivery through the PostgreSQL outbox
- Backend tests: JUnit 5 and Mockito, with PostgreSQL-backed integration tests

The approved architecture is a single modular monolith. Do not introduce microservices, message brokers, or unrelated infrastructure.

## Approved sources of truth

Treat these approved documents as the requirements and design baseline:

1. `docs/requirements.md` (M1)
2. `docs/architecture.md` (M2.1)
3. `docs/database-design.md` (M2.2)
4. `docs/api-specification.md` (M2.3)

Read the relevant SDD documents, area README, implementation plan, and QA plan before implementation. Follow the API and database contracts. Never silently modify an approved specification; propose a specific change and obtain explicit owner approval first.

## SDD-first workflow and scope

1. Inspect the workspace, current Git changes, applicable `AGENTS.md` files, Skills, and existing tests.
2. Read the relevant specifications and trace the task to requirements and acceptance criteria.
3. Identify dependencies, affected modules, risks, and verification steps. Explain the plan before substantial work.
4. If an essential product, security, API, or data decision is unclear, ask the project owner. Continue independent work where possible.
5. Implement only the assigned, approved behavior in a focused change.
6. Run relevant checks after implementation. Report exact commands, outcomes, changed files, and unresolved questions. Never claim an unrun check passed.

Do not invent requirements, business rules, endpoints, fields, response behavior, schema, or deployment assumptions. Do not implement unapproved features or edit approved specifications as a side effect of code changes.

## Java and Spring Boot standards

- Keep HTTP handling in controllers, use cases and business rules in services, and persistence in repositories.
- Use request and response DTOs; do not expose JPA entities directly.
- Prefer constructor injection, focused classes, clear names, and explicit error handling.
- Validate at API boundaries and enforce important invariants in service and database layers.
- Use transactions for registration plus outbox creation, capacity changes, and other multi-step state changes that must succeed together.
- Follow established Maven and Spring conventions. Add dependencies only for an approved task and reason.

## React standards

- Follow the existing React structure and tooling. Do not add a state-management or UI library without approval.
- Keep API access and feature UI responsibilities clear; follow the approved API contract and handle loading, empty, success, and error states.
- Use accessible labels, keyboard behavior, and semantic elements.
- Treat frontend validation as usability support; the backend remains authoritative.
- Never put session identifiers, credentials, or secrets in browser local storage or source code.

## Architecture and REST API rules

- Keep the approved single modular monolith and separation between presentation, application/business logic, persistence, and integrations.
- Implement only routes and behavior in `docs/api-specification.md` under `/api/v1`.
- Follow its JSON DTO fields, validation limits, HTTP status codes, and standard error contract. Do not add pagination, routes, filters, or response fields outside the approved API.
- Use explicit-offset API timestamps and the approved `Asia/Phnom_Penh` time-zone semantics.
- Keep participant data out of responses unless the caller is an authorized Admin or the Instructor assigned to that workshop.

## PostgreSQL and data integrity

- Use only the four approved tables: `accounts`, `workshops`, `registrations`, and `email_outbox`.
- Follow `docs/database-design.md` for identity primary keys, columns, field limits, foreign keys, checks, unique constraints, indexes, and deletion behavior.
- Store workshop times as unambiguous instants; interpret and display them using `Asia/Phnom_Penh`.
- Derive workshop status; do not persist a separate status column. Reject registration at or after workshop start.
- Prevent duplicate registration with the approved unique constraint and prevent overbooking with the approved transaction and workshop-row lock strategy.
- Prevent workshop deletion while registrations exist and prevent capacity reductions below the registration count.
- Insert the registration and pending outbox message in one transaction. Follow the approved retry defaults and retention behavior.
- Put database scripts and reviewed migrations under `database/` unless an established build convention requires otherwise. Never create unapproved schema objects, triggers, or migrations, or execute SQL against a production database.

## Security requirements

- Use Spring Security server-side sessions, secure cookies, HTTPS, CSRF protection for state-changing requests, and exact-origin CORS if frontend and API use separate origins.
- Enforce Admin and Instructor roles on the server; UI route guards are not authorization.
- Derive the current Instructor from the authenticated session and verify workshop ownership for every protected Instructor workshop and participant request.
- Store password hashes only. Keep database and SMTP credentials in external configuration.
- Do not expose participant data, password hashes, secrets, stack traces, SQL details, or sensitive outbox errors to unauthorized callers.
- The CSRF token transport/header, session timeout, and final `SameSite` value remain open configuration decisions in the approved documents. Do not silently choose them or add an unapproved CSRF endpoint.

## Testing and quality

- Add or update JUnit 5 and Mockito unit tests for changed backend behavior.
- Add integration tests when persistence, database constraints, authentication, authorization, or transactions are involved. Use PostgreSQL-backed tests for PostgreSQL-specific behavior.
- Cover success, validation, authorization, conflict, boundary, concurrency, and failure cases linked to acceptance criteria.
- Use the established frontend test tooling for React changes and verify accessible, user-visible states.
- Keep repeatable QA plans, cases, reports, and manual test notes in `qa/`. Do not rely on production services or real user data.

## Git and change management

- Inspect `git status` before editing and preserve unrelated user changes.
- Keep diffs focused. Do not stage, commit, push, reset, or rewrite history unless the owner explicitly asks.
- Never commit credentials, local environment files, build outputs, caches, or real participant data.
- When asked to commit, use a clear imperative Conventional Commit subject such as `docs: ...`, `feat: ...`, `fix: ...`, or `test: ...`.

## Owner decisions and agent conduct

- Ask the owner when an unsettled decision affects product behavior, authorization, API contracts, persisted data, security policy, or scope. Explain the impact and offer a small set of options.
- Inspect relevant files before editing and create only what the task requires. Preserve existing work and do not overwrite user material unnecessarily.
- Keep implementation traceable to approved specifications and acceptance criteria. Report changed files, tests/checks run, and unresolved items.
