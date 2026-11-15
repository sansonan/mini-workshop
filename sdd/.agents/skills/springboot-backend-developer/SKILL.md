---
name: springboot-backend-developer
description: Implement a specifically assigned and approved backend task with Java 21, Spring Boot, Maven, PostgreSQL, Spring Security, JUnit 5, and Mockito. Use only after reading the applicable SDD contracts.
---

# Spring Boot Backend Developer

## Scope

Implement only the backend task assigned by the project owner. This Skill does not authorize new product behavior, API routes, database schema, dependencies, or changes to approved specifications.

## Use this Skill when

- The owner asks for an approved backend feature, defect fix, or test change.
- The task is traceable to the approved requirements, architecture, database design, and API specification.

Do not use it for requirements discovery, architecture changes, database design approval, frontend-only work, or read-only code review.

## Workflow

1. Read the root `AGENTS.md`, this Skill, and relevant sections of the four approved SDD documents.
2. Inspect the existing code, build configuration, current Git changes, and nearby tests. Do not assume a package, dependency, endpoint, or schema that is not present or approved.
3. Summarize the implementation plan, files/components affected, contract constraints, and tests before substantial work. Ask the owner if a necessary decision is unclear.
4. Implement only the assigned scope, preserving existing conventions and unrelated changes.
5. Add or update tests that cover the approved behavior and its failure/boundary cases.
6. Run relevant Maven tests/checks and report exact commands and results. Review the final diff and list changed files.

## Implementation responsibilities

### Layer responsibilities

- **Controller:** HTTP mapping, request DTO binding, validation, and response/status selection. Keep business logic out of controllers.
- **Service:** use cases, business rules, authorization-sensitive decisions, transaction boundaries, and coordination with the outbox.
- **Repository:** persistence queries and locking/constraint-aware operations; do not hide product rules in ad hoc SQL.
- **Entity/model:** represent only approved persisted data. Do not add fields or status columns absent from M2.2.
- **DTO:** define API inputs and outputs exactly from M2.3; do not expose persistence entities.

### Validation and error handling

- Apply request validation using established Spring conventions and M2.3 field limits.
- Trim and normalize emails consistently with M2.2; validate syntax in the service/request boundary.
- Map failures to the approved JSON error shape and stable error codes. Do not return stack traces or internal database/SMTP details.
- Preserve the specified HTTP status codes for validation, authentication, authorization, missing resources, and conflicts.

### Transactions and database integrity

- Use one transaction to lock a workshop row, validate time/capacity/duplicate conditions, insert the registration, and insert its pending email outbox row.
- Use the approved row-level lock for capacity changes and reject reductions below current registrations.
- Rely on PostgreSQL foreign keys, unique constraints, and checks as the final integrity boundary; handle expected constraint conflicts cleanly.
- Keep workshop status derived from time and registration count. Apply `Asia/Phnom_Penh` to approved calculations and display conversions.
- Respect restrictive deletion behavior. Do not add triggers, columns, tables, or migrations unless the owner approves a design change.

### Authentication and authorization

- Use Spring Security server-side sessions and the approved secure cookie, CSRF, and CORS behavior.
- Enforce role access on the server. For Instructor requests, derive the account from the session and verify workshop ownership before returning workshops or participants.
- Validate instructor assignment in the service layer by confirming that the account has the `INSTRUCTOR` role.
- Never trust an account or role supplied by the frontend as proof of authorization.

### Testing

- Use JUnit 5 and Mockito for unit tests and the established repository integration-test setup for database/security behavior.
- Cover successful behavior, validation, duplicate registration, full/started workshops, concurrent capacity protection, role checks, Instructor ownership, deletion conflicts, and outbox creation as applicable to the task.
- Do not claim a test passed unless it was executed. Explain failures and environmental skips accurately.
