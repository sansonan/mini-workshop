---
name: sdd-document-reviewer
description: Review SDD requirements, architecture, database, API, security, and acceptance-criteria traceability for gaps and conflicts. Use for specification audits; do not use to implement application code.
---

# SDD Document Reviewer

## Scope

Review project specifications and explain whether they are complete and consistent. This Skill is read-only: do not edit approved documents unless the project owner explicitly authorizes a specific edit.

## Use this Skill when

- The project owner requests an SDD document review or consistency check.
- A new requirement, architecture decision, database design, or API contract needs review against existing approved material.
- The team wants traceability from requirements and acceptance criteria to design documents.

Do not use this Skill to implement backend/frontend code, modify specifications, or perform a code-only review.

## Workflow

1. Read the relevant root `AGENTS.md` and all source documents needed for the review. Treat approved documents as the source of truth.
2. Identify document versions/status and define the review scope. Do not assume a draft supersedes an approved document.
3. Trace requirements and acceptance criteria to architecture, database, and API sections.
4. Check for missing requirements, conflicting business rules, architecture inconsistencies, database relationship/constraint gaps, API mismatches, security/authorization gaps, and untraceable acceptance criteria.
5. Check whether proposed recommendations introduce unapproved behavior, scope, tables, endpoints, or security assumptions.
6. Report findings without modifying files. If no defects are found, state the limits of the review and list useful recommendations separately.

## Finding format

For each finding, provide:

- **Severity:** Critical, High, Medium, or Low.
- **Type:** Confirmed defect or Recommendation.
- **Reference:** Exact file and section, and line when available.
- **Finding:** What is missing or inconsistent.
- **Impact:** Why it matters to implementation or verification.
- **Suggested resolution:** A concrete change or owner decision; do not apply it without authorization.

Distinguish a confirmed contradiction or omission from a preference. Do not label a recommendation as a defect. Avoid restating entire documents; focus on actionable findings and traceability.
