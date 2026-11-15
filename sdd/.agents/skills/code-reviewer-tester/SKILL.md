---
name: code-reviewer-tester
description: Perform a read-only quality, security, contract, and test review of implemented Mini Workshop Booking System code. Use after implementation or when asked to verify a change; do not modify code automatically.
---

# Code Reviewer and Tester

## Scope

Review code against the approved SDD documents and report actionable findings. The review is read-only: do not fix or reformat code unless the owner separately authorizes an implementation task.

## Use this Skill when

- The owner requests a code review, test run, regression check, or implementation verification.
- A change needs review for Java, React, security, API contract, database, or acceptance-criteria compliance.

Do not use this Skill to implement features, change approved specifications, or silently alter failing tests.

## Workflow

1. Read root `AGENTS.md`, this Skill, the four approved SDD documents relevant to the change, and applicable tests.
2. Inspect Git status and the complete diff so pre-existing user work is not confused with the submitted change.
3. Review the change for correctness, scope, regressions, authorization, data integrity, and contract compliance.
4. Run only relevant tests/checks that are available and safe. Record exact commands, outcomes, and environment-related skips.
5. Analyze failures to identify whether they are caused by the change, an existing defect, test setup, or environment. Do not change code during review.
6. Report findings by severity, followed by test results and recommendations. If no findings are identified, state the review scope and checks performed.

## Review checklist

- **Java/Spring Boot:** clear controller/service/repository boundaries, DTO use, validation, transaction scope, error mapping, and maintainable code.
- **React:** API contract use, accessible controls, loading/empty/error/success states, client-side assumptions, and leakage of secrets/session data.
- **Security:** server-side session handling, CSRF and cookie settings, role checks, Instructor ownership enforcement, PII access, password hashes, and secret handling.
- **API contract:** paths, methods, fields, date/time offsets, derived status behavior, status codes, and error structure match M2.3.
- **Database integrity:** use of approved tables, field limits, foreign keys, unique/check constraints, deletion rules, email normalization, locking, and outbox behavior match M2.2.
- **Tests:** relevant unit and integration coverage, boundary/failure cases, regression risks, and whether reported results match commands actually run.

## Report format

Group findings under:

- **Critical issues** — exploitable security problems, data loss/corruption, or release-blocking behavior.
- **High-priority issues** — major contract or business-rule violations or serious regressions.
- **Medium-priority issues** — meaningful correctness, maintainability, or coverage gaps.
- **Low-priority suggestions** — optional improvements with limited immediate risk.

For every finding, include severity, confirmed defect versus possible improvement, file/line reference, impact, and a recommended action. Do not inflate recommendations into defects.

Then report:

- Tests/checks executed, exact commands, pass/fail results, and skips.
- Failure analysis and regression concerns.
- Remaining recommendations or open questions.

Never claim tests passed unless they actually ran. Never automatically change code during review.
