# Mini Workshop Booking System — API Specification

| Document information | Value |
|---|---|
| Project | Mini Workshop Booking System |
| Document | REST API Specification |
| Status | Approved |
| Sources of truth | `requirements.md` (M1), `architecture.md` (M2.1), `database-design.md` (M2.2) |
| API version | v1 |

This document specifies the HTTP contract only. It does not implement endpoints or define Java/React code, SQL migrations, or tests. API field lengths and persistence behavior follow M2.2. Workshop status, authentication, and endpoint boundaries follow M2.1 and M1.

## 1. API Overview

### REST conventions

- Base path: `/api/v1`.
- Use resource-oriented paths and HTTP methods: `GET` reads; `POST` creates or performs registration/login; `PUT` replaces an editable workshop; `DELETE` deletes a permitted workshop.
- Requests and responses use `application/json`, except responses with no body such as `204 No Content`.
- JSON property names use `camelCase`; IDs are decimal integers; booleans are JSON booleans.
- Date/time request and response values use ISO 8601/RFC 3339 timestamps with an explicit offset. Workshop values are interpreted and displayed in `Asia/Phnom_Penh` (UTC+7), for example `2027-06-18T09:00:00+07:00`. Do not send timezone-less local date/time values.
- Workshop `status` is included in read responses as a derived value. It is not accepted as a persisted or editable field.
- Public workshop responses never include participant data.
- Admin and Instructor APIs use the authenticated server-side session cookie described in Section 10.

### HTTP status conventions

| Status | Use |
|---|---|
| `200 OK` | Successful read, login, current-user lookup, or workshop update with a response body. |
| `201 Created` | Successful workshop creation or visitor registration. Include a `Location` header for the created resource when its resource URL is available. |
| `204 No Content` | Successful workshop deletion. |
| `400 Bad Request` | Malformed JSON, invalid field values, unsupported filters, or validation failure. |
| `401 Unauthorized` | Missing/invalid session or invalid login credentials. |
| `403 Forbidden` | Authenticated user lacks the required role/ownership, or CSRF validation fails. |
| `404 Not Found` | Requested workshop or resource does not exist. |
| `409 Conflict` | Duplicate registration, full/started/ended workshop registration attempt, or deletion blocked by registrations. |
| `500 Internal Server Error` | Unexpected server error; response must not expose stack traces or secrets. |

## 2. Authentication APIs

Authentication is for Admin and Instructor accounts only. Visitors do not create accounts.

| Method and path | Access | Success |
|---|---|---|
| `POST /api/v1/auth/login` | Public, CSRF protected | `200 OK`, sets the session cookie and returns the authenticated staff account. |
| `POST /api/v1/auth/logout` | Authenticated, CSRF protected | `204 No Content`, invalidates the session and expires its cookie. |
| `GET /api/v1/auth/me` | Authenticated | `200 OK`, returns the current staff account and role. |

Login request:

```json
{
  "email": "admin@example.com",
  "password": "<submitted password>"
}
```

Login response body (the session cookie is returned in `Set-Cookie`, not in JSON):

```json
{
  "accountId": 12,
  "email": "admin@example.com",
  "displayName": "Workshop Admin",
  "role": "ADMIN"
}
```

`GET /api/v1/auth/me` returns the same account representation. Invalid credentials return `401` with `INVALID_CREDENTIALS`; do not reveal whether the email or password was incorrect. Inactive accounts must not receive an authenticated session.

## 3. Public Workshop APIs

### List workshops

`GET /api/v1/workshops`

Returns public workshop details for Open, Full, and Completed workshops. This matches the approved architecture, which says visitors can identify Full and Completed workshops. Only workshops satisfying registration eligibility may be booked.

Optional filter:

| Query parameter | Values | Behavior |
|---|---|---|
| `status` | `OPEN`, `FULL`, `COMPLETED` | Filters by the current derived status. Omit to return all statuses. Invalid values return `400`. |

Pagination is not defined in M1, M2.1, or M2.2, so v1 does not specify page/size parameters or a paginated response envelope. Add pagination only after its limits and response shape are approved.

Example response:

```json
{
  "workshops": [
    {
      "workshopId": 42,
      "title": "Introduction to Woodworking",
      "description": "A beginner workshop covering safe tool use and basic joinery.",
      "startsAt": "2027-06-18T09:00:00+07:00",
      "endsAt": "2027-06-18T12:00:00+07:00",
      "location": "Phnom Penh Training Center, Room 2",
      "instructor": {
        "displayName": "Sokha Chan"
      },
      "maxParticipants": 16,
      "availableSeats": 5,
      "status": "OPEN"
    }
  ]
}
```

### View workshop details

`GET /api/v1/workshops/{workshopId}`

Returns the same public-safe workshop representation as a list item. Return `404 WORKSHOP_NOT_FOUND` if no such workshop exists. Do not return participant names, emails, or phone numbers.

## 4. Registration APIs

### Register for a workshop

`POST /api/v1/workshops/{workshopId}/registrations`

No visitor login is required. The path identifies the workshop.

| JSON field | Required | Limits / validation |
|---|---:|---|
| `fullName` | Yes | 1–200 characters after trimming; reject blank/whitespace-only values. |
| `email` | Yes | Up to 320 characters; validate email syntax, trim, and lowercase with `Locale.ROOT` before persistence. |
| `phoneNumber` | No | Null or omitted is allowed; up to 40 characters when supplied; reject whitespace-only values. |

Example request:

```json
{
  "fullName": "Dara Vann",
  "email": "Dara.Vann@example.com",
  "phoneNumber": "+855 12 345 678"
}
```

The service normalizes the email before checking duplicates and saving. In one database transaction it locks the workshop row, checks eligibility and capacity, inserts the registration, and inserts the pending email outbox record. The API returns success only after the transaction commits. The email is processed asynchronously through the outbox.

Example `201 Created` response:

```json
{
  "registrationId": 901,
  "workshopId": 42,
  "registeredAt": "2027-06-01T14:25:10+07:00",
  "message": "Registration confirmed. A confirmation email has been queued."
}
```

The response does not include the participant record beyond the identifier and does not imply that SMTP delivery has already completed.

### Registration conflicts

| Condition | HTTP | Error code | Behavior |
|---|---:|---|---|
| Same normalized email already registered for this workshop | `409` | `DUPLICATE_REGISTRATION` | Do not create another registration or outbox record. |
| Registration count has reached capacity and the workshop has not ended | `409` | `WORKSHOP_FULL` | Do not create a registration; derived status is `FULL`. |
| Current time is at or after workshop start | `409` | `REGISTRATION_CLOSED` | Reject regardless of displayed status. |
| Workshop does not exist | `404` | `WORKSHOP_NOT_FOUND` | No registration is created. |
| Workshop end time has been reached | `409` | `REGISTRATION_CLOSED` | The workshop is `COMPLETED`; no registration is created. |

At exactly the start instant, registration is rejected. Capacity is checked under the workshop row lock and is protected by `UNIQUE (workshop_id, email_normalized)` as a second defense against duplicate requests.

## 5. Admin APIs

All routes in this section require an authenticated `ADMIN` account.

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/v1/admin/workshops` | View all workshops with derived status and available seats. | `200 OK` |
| `POST /api/v1/admin/workshops` | Create a workshop and assign an existing Instructor using `instructorId`. | `201 Created` |
| `PUT /api/v1/admin/workshops/{workshopId}` | Replace editable workshop fields; `instructorId` may change the assignment. | `200 OK` |
| `DELETE /api/v1/admin/workshops/{workshopId}` | Delete a workshop only when it has no registrations. | `204 No Content` |
| `GET /api/v1/admin/workshops/{workshopId}/registrations` | View participants and registration dates for a workshop. | `200 OK` |

Workshop create/update request fields:

| JSON field | Required | Limits / validation |
|---|---:|---|
| `title` | Yes | 1–200 characters after trimming; not blank. |
| `description` | Yes | 1–5,000 characters after trimming; not blank. |
| `startsAt` | Yes | ISO 8601 timestamp with explicit offset, interpreted/displayed in `Asia/Phnom_Penh`. |
| `endsAt` | Yes | ISO 8601 timestamp with explicit offset; must be later than `startsAt`. |
| `location` | Yes | 1–300 characters after trimming; not blank. |
| `maxParticipants` | Yes | Integer greater than zero; cannot be updated below the current registration count. |
| `instructorId` | Yes | ID of an existing account whose role is `INSTRUCTOR`. |

There is no `status` request field. The API derives `status` from time and registration count. Instructor assignment is performed through `instructorId` on create/update; no separate assignment route is defined in the approved architecture.

Example create request:

```json
{
  "title": "Introduction to Woodworking",
  "description": "A beginner workshop covering safe tool use and basic joinery.",
  "startsAt": "2027-06-18T09:00:00+07:00",
  "endsAt": "2027-06-18T12:00:00+07:00",
  "location": "Phnom Penh Training Center, Room 2",
  "maxParticipants": 16,
  "instructorId": 27
}
```

Example response is the public workshop representation plus its identifier and derived status. `PUT` uses the same complete field set; partial updates are not specified.

Admin participant response example:

```json
{
  "workshopId": 42,
  "participants": [
    {
      "registrationId": 901,
      "fullName": "Dara Vann",
      "email": "dara.vann@example.com",
      "phoneNumber": "+855 12 345 678",
      "registeredAt": "2027-06-01T14:25:10+07:00"
    }
  ]
}
```

If deleting a workshop with registrations, return `409 WORKSHOP_HAS_REGISTRATIONS`. If an instructor ID does not exist or does not belong to an Instructor account, return a validation error; do not create or update the workshop.

## 6. Instructor APIs

All routes require an authenticated `INSTRUCTOR` account. Results are scoped to the current account, not to a client-supplied instructor ID.

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/v1/instructor/workshops` | View only workshops assigned to the current Instructor. | `200 OK` |
| `GET /api/v1/instructor/workshops/{workshopId}/registrations` | View participants only if that workshop is assigned to the current Instructor. | `200 OK` |

For an unassigned workshop, return `403 FORBIDDEN`. Never return participant data before checking assignment in the backend. Instructors cannot create, edit, delete workshops, or change registrations.

## 7. Error Response Contract

All API errors use this JSON shape:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid.",
  "path": "/api/v1/admin/workshops",
  "fieldErrors": [
    {
      "field": "maxParticipants",
      "code": "MUST_BE_POSITIVE",
      "message": "Must be greater than zero."
    }
  ]
}
```

`fieldErrors` is included for request validation errors and omitted or empty for non-field errors. Messages are safe for clients; never include SQL, stack traces, password details, SMTP credentials, or other secrets.

| HTTP | Error codes (examples) | Meaning |
|---:|---|---|
| `400` | `VALIDATION_ERROR`, `MALFORMED_JSON`, `INVALID_FILTER` | Request cannot be processed because its input is malformed or invalid. |
| `401` | `UNAUTHENTICATED`, `INVALID_CREDENTIALS` | No valid session or login failed. |
| `403` | `FORBIDDEN`, `CSRF_REJECTED` | Authenticated user lacks access, or CSRF validation failed. |
| `404` | `WORKSHOP_NOT_FOUND`, `RESOURCE_NOT_FOUND` | Requested resource does not exist. |
| `409` | `DUPLICATE_REGISTRATION`, `WORKSHOP_FULL`, `REGISTRATION_CLOSED`, `WORKSHOP_HAS_REGISTRATIONS`, `CAPACITY_BELOW_REGISTRATIONS` | Request conflicts with current workshop or registration state. |
| `500` | `INTERNAL_ERROR` | Unexpected failure without internal details in the response. |

For field validation, use stable field names and codes, for example `REQUIRED`, `TOO_LONG`, `INVALID_EMAIL`, `MUST_BE_POSITIVE`, and `MUST_BE_AFTER_START`. The server remains authoritative even when the React client performs validation.

## 8. Authorization Rules

| Role | Allowed API access |
|---|---|
| Visitor (unauthenticated) | Public workshop list/details and workshop registration only. No participant or staff data. |
| Admin | Authentication endpoints, all Admin workshop operations, and participant listings for any workshop. |
| Instructor | Authentication endpoints, assigned-workshop list, and participant list for assigned workshops only. No workshop management or registration changes. |

Authorization is enforced by Spring Security and backend service/query checks. The API does not trust role, account ID, or instructor ID supplied by React. Instructor routes derive the current account from the authenticated session and verify workshop assignment before returning participants.

## 9. Request and Response Examples

### Derived workshop status

The status field is response-only:

- `COMPLETED` when current time is at or after `endsAt`.
- `FULL` when current time is before `endsAt` and registration count has reached `maxParticipants`.
- `OPEN` otherwise.

Status and registration calculations use `Asia/Phnom_Penh`. A workshop may display `OPEN` after it starts but before it ends if it still has capacity; registration is nevertheless rejected at or after `startsAt`.

### Duplicate registration error

```json
{
  "status": 409,
  "code": "DUPLICATE_REGISTRATION",
  "message": "This email is already registered for the workshop.",
  "path": "/api/v1/workshops/42/registrations"
}
```

### Registration after start error

```json
{
  "status": 409,
  "code": "REGISTRATION_CLOSED",
  "message": "Registration is closed because the workshop has started.",
  "path": "/api/v1/workshops/42/registrations"
}
```

### Full workshop error

```json
{
  "status": 409,
  "code": "WORKSHOP_FULL",
  "message": "The workshop has reached its maximum capacity.",
  "path": "/api/v1/workshops/42/registrations"
}
```

## 10. API Security

- Use Spring Security with server-side sessions; do not place session identifiers in JSON or browser local storage.
- Return the session ID only in a cookie marked `HttpOnly` and `Secure` over HTTPS, with a `SameSite` policy appropriate to the deployment. The approved architecture prefers React and API on the same site; if they are separate origins, allow only the exact frontend origin and use credentialed requests.
- Require CSRF protection for state-changing requests. The React client must send the configured CSRF token in the configured request header. Login/logout and cookie-authenticated write requests must follow the configured Spring Security policy.
- Do not expose database, password hash, SMTP, or outbox diagnostic details through public responses.
- Validate Instructor ownership and Admin role on the backend for every protected operation. UI route guards are not a security boundary.
- Keep database and SMTP credentials in external configuration, never in this API contract or source control.

The architecture does not define how the React application obtains a CSRF token, the exact CSRF header/cookie name, session timeout, or the final `SameSite` value. Select these in the security configuration before implementation; do not add an undocumented API endpoint for token retrieval.

## 11. API-to-Database Mapping

| API operation | Database support |
|---|---|
| Login, logout, current user | `accounts` supplies normalized email, password hash, role, and active flag. Session lifecycle is managed by Spring Security, not by a new application table. |
| Public/Admin/Instructor workshop lists and details | `workshops` and its Instructor FK to `accounts`; count `registrations` to calculate available seats and derived status. Instructor list queries filter by the current account ID. |
| Create/update workshop and assign Instructor | Insert/update `workshops`; validate the `instructor_id` account has `INSTRUCTOR` role in the Spring Boot service. Capacity changes lock the workshop row and cannot go below its registration count. |
| Delete workshop | Delete from `workshops`; the `registrations.workshop_id` restrictive FK prevents deletion when registrations exist. |
| Visitor registration | Lock and read `workshops`; validate time/capacity; insert `registrations`; unique `(workshop_id, email_normalized)` prevents duplicate booking. |
| Admin/Instructor participant list | Read `registrations` for the requested workshop; Instructor query additionally verifies the workshop's `instructor_id` matches the current account. |
| Confirmation email | Insert `email_outbox` recipient, subject, and body in the registration transaction; delivery worker updates attempt count, next attempt, status, sent time, and last error. No extra queue table is introduced. |

## Approved API Decisions

- V1 workshop and participant lists are unpaginated. Public workshop listing may be filtered by derived `status`.
- The JSON error structure and stable error codes in Section 7 are approved.
- An Instructor requesting another Instructor's workshop receives `403 FORBIDDEN`, consistent with the architecture's rule for authenticated users without permission.
- Workshop updates use full replacement with `PUT`; partial `PATCH` updates are not part of this API contract.
- Session authentication, CSRF protection, secure cookie attributes, same-site deployment preference, and exact-origin CORS behavior follow M2.1. No CSRF bootstrap API endpoint is added.

The CSRF token transport/header, session timeout, and final `SameSite` value remain Spring Security deployment configuration because M2.1 leaves these values context-dependent. They do not change the approved endpoint contract and must be configured before implementation.

No new product features are introduced. The API reuses the approved routes and four-table data model; the outbox remains internal to the REST contract.
