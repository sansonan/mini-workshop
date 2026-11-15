# Reusable Agent Skills

Codex discovers project Skills from folders under `.agents/skills/` when `sdd/` is open as the workspace. Each Skill is defined by a `SKILL.md` file with YAML frontmatter containing its name and description.

## Available Skills

- `sdd-document-reviewer/` — review approved requirements, architecture, database design, API contracts, security, and acceptance-criteria traceability. Read-only unless the owner explicitly authorizes specification changes.
- `springboot-backend-developer/` — implement specifically assigned and approved backend work using Java 21, Spring Boot, Maven, PostgreSQL, and the approved security and testing conventions.
- `code-reviewer-tester/` — perform read-only code, security, contract, and test reviews; report findings by severity and never claim unexecuted tests passed.

Keep Skills focused on reusable task workflows. Follow `../AGENTS.md` and the approved specifications; do not duplicate or override project-wide rules.
