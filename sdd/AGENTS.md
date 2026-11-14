# Project Instructions for AI Agents

These instructions apply to work inside `sdd/`. They are intended for Codex and other AI agents collaborating on this project.

## Project overview

This is the foundation for a Spec-Driven Development project. The planned application uses a Java 21 and Spring Boot backend, a React frontend, and PostgreSQL. This directory currently establishes organization and guidance only; do not treat the planned stack as permission to invent product behavior or start implementation without an approved specification.

## Read specifications before implementation

Before changing code, data models, APIs, or behavior:

1. Read this file and the README for the area you will change.
2. Find and read the relevant requirements, acceptance criteria, architecture, API, data model, and decision records in `docs/` and `database/`.
3. Check for an implementation plan or QA plan related to the requested work.
4. If an essential requirement is missing or conflicting, make progress on independent documentation or investigation and ask for clarification before making assumptions that affect product behavior or architecture.
5. Keep changes traceable to the specifications; update the related documentation when an approved decision changes.

## SDD development principles

- Treat reviewed specifications and acceptance criteria as the source of truth.
- Define observable outcomes and edge cases before implementing them.
- Keep requirements, design decisions, implementation, and verification linked.
- Prefer small, reviewable changes that satisfy a stated requirement.
- Record meaningful decisions and their rationale in `docs/`.
- Do not add application functionality as part of this foundation task.

## Technology and coding standards

- Backend code belongs in `backend/` and targets Java 21 with Spring Boot.
- Frontend code belongs in `frontend/` and uses React.
- Database scripts, schema assets, migrations, and SQL guidance belong in `database/`.
- Follow the existing build tools, dependency versions, formatting, and naming conventions once established. Do not introduce a second tool or convention without documenting the reason.
- Prefer clear names, focused modules, explicit validation, and useful error handling.
- Keep configuration external where appropriate. Never commit credentials, tokens, private keys, or production data.
- Keep API contracts explicit and consistent across frontend, backend, and specifications.
- Make user-facing interfaces accessible and handle loading, empty, error, and success states where relevant.

## Architecture guidelines

- Use the architecture documented in `docs/`; do not create competing patterns without recording and reviewing the decision.
- Keep presentation, application/business logic, persistence, and integration responsibilities clearly separated.
- Validate inputs at system boundaries and enforce important invariants in the appropriate domain or service layer.
- Avoid unnecessary dependencies, hidden cross-layer coupling, and duplicated sources of truth.
- Document API changes, compatibility effects, and significant data-flow changes.

## Testing and quality requirements

- For each behavior change, add or update tests at the appropriate level and map coverage to acceptance criteria.
- Run the relevant project checks after implementation and report exactly what was run and its result. Do not claim checks passed if they were not run.
- Keep test plans, cases, reports, and manual QA notes in `qa/` when they are project artifacts.
- Tests must be repeatable and must not rely on production systems or real user data.
- Include validation, failure paths, and boundary cases, not only the happy path.

## Database guidelines

- Use PostgreSQL and keep database work under `database/` unless the established build structure requires otherwise.
- Make schema changes through reviewed, ordered, versioned migrations; do not rely on undocumented manual edits.
- Consider constraints, indexes, nullability, data integrity, and migration/rollback impact.
- Never put secrets or real personal data in scripts, fixtures, or documentation.
- Update the data model and relevant API or architecture specifications when persisted data changes.

## Git and commit conventions

- Keep changes focused and avoid unrelated formatting or generated files.
- Use imperative, descriptive commit subjects. Prefer Conventional Commit prefixes such as `docs:`, `feat:`, `fix:`, `test:`, and `refactor:` when the repository adopts that convention.
- Do not commit local environment files, credentials, build outputs, dependency caches, or test artifacts unless explicitly required and documented.
- Preserve existing user changes. Review the working tree before editing or staging files.

## Rules for AI agents

- Inspect the relevant project structure and existing files before editing.
- Create only what the task requires; do not overwrite existing material without a clear need.
- Do not invent requirements, endpoint behavior, schema details, or deployment assumptions. Mark unresolved choices as open questions.
- Do not implement unrelated functionality or add dependencies without a specification or explicit request.
- Keep documentation beginner-friendly, accurate, and synchronized with the project.
- Summarize files changed, checks performed, and any unresolved questions in the final response.
