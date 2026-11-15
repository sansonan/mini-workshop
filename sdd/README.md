# Spec-Driven Development Project

This directory is the main workspace for the Mini Workshop Booking System. It contains the approved SDD documents, project-wide AI instructions, reusable Skills, and dedicated locations for backend, frontend, database, and QA work. The application foundation is documented; application functionality is not implemented yet.

## Objectives

- Agree on requirements and acceptance criteria before implementation.
- Keep architectural decisions and implementation plans easy to find and review.
- Separate backend, frontend, database, and QA responsibilities.
- Give people and AI agents consistent, project-wide working instructions.

## Technology stack

- Backend: Java 21, Spring Boot, and Maven
- Frontend: React
- Database: PostgreSQL (`workshop_booking_db`)
- API: JSON REST under `/api/v1`
- Development approach: Spec-Driven Development (SDD)

Use the versions and conventions established by the backend and frontend projects when they are initialized. Keep credentials and environment-specific configuration out of source control.

## Project structure

```text
sdd/
├── docs/             Project specifications, architecture, plans, and decisions
├── agents/           AI agent roles, responsibilities, and workflows
├── .agents/skills/   Reusable agent skills and task workflows
├── backend/          Java 21 and Spring Boot application
├── frontend/         React application
├── database/         PostgreSQL schema, migrations, and SQL documentation
├── qa/               Test plans, cases, reports, and QA documentation
├── AGENTS.md         Instructions for Codex and other AI agents
└── README.md         Project overview and getting-started guide
```

The `agents/`, `backend/`, `frontend/`, `database/`, and `qa/` directories contain README files describing their intended contents. The approved specifications live in `docs/`, and `.agents/skills/README.md` lists the reusable Skills. Open `sdd/` as the Codex workspace so Codex discovers `AGENTS.md` and Skills under `.agents/skills/`. Add implementation files only after the applicable specifications and plans are reviewed.

## Development environment prerequisites

Install the following before implementation begins:

- Git
- JDK 21
- Maven (or the project Maven Wrapper once added)
- Node.js and the package manager selected by the React project
- PostgreSQL, locally or through an approved development service

Record exact versions and additional tools in the relevant project documentation once chosen.

## Setup

1. Clone or open the repository, then open `sdd/` as the project workspace.
2. Read `AGENTS.md` and the relevant approved documents under `docs/`.
3. Configure local environment values using the project's documented template. Do not commit secrets.
4. Install backend and frontend dependencies after their respective application manifests have been created.
5. Create or connect to a PostgreSQL development database using the documented database setup and migrations.

This foundation does not include application manifests, source code, database migrations, or a runnable service yet. The commands below become available after those components are specified and added.

## Run the backend

From `backend/`, run the Spring Boot application with Maven after its project files and configuration have been created:

- Maven: `mvn spring-boot:run`
- Maven Wrapper, if present: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)

The backend README and run configuration should document the chosen command, required environment variables, and expected local address once the application exists.

## Run the frontend

From `frontend/`, install dependencies and start the development server using the package manager and scripts defined in `package.json`. For an npm-based setup, the typical commands are `npm install` followed by `npm run dev` or `npm start`, depending on the selected React tooling.

Document the actual commands and local address in the frontend README when the application is initialized.

## Database configuration

Use PostgreSQL database `workshop_booking_db` for local development and keep connection details outside source control. The backend configuration should document environment variable names, such as a JDBC URL, username, and password, without storing real credentials in the repository. Keep schema changes in reviewed, versioned migrations under `database/`; see that directory's README for guidance.

## SDD development workflow

1. Capture the problem, users, scope, and measurable acceptance criteria in `docs/`.
2. Review requirements and resolve open questions before implementation.
3. Record architecture, API contracts, data model, and technical decisions.
4. Create a small implementation plan that links work to the approved specifications.
5. Implement backend, frontend, and database changes within their designated areas.
6. Add or update QA plans and automated tests for the behavior being changed.
7. Review the implementation against the specifications, run the documented checks, and record outcomes where useful.
8. Update specifications and decision records when approved requirements or design choices change.

See `AGENTS.md` for the detailed rules that apply to future work.
