# Mini Workshop Booking System

## Product Requirements Document (PRD)

| Document Information | Value                         |
| -------------------- | ----------------------------- |
| Project Name         | Mini Workshop Booking System  |
| Document             | Product Requirements Document |
| Version              | 1.0                           |
| Status               | Approved                      |
| Development Approach | Spec-Driven Development (SDD) |
| Backend              | Java 21, Spring Boot, Maven   |
| Frontend             | React                         |
| Database             | PostgreSQL                    |
| API                  | REST API                      |
| Testing              | JUnit 5, Mockito              |

---

## 1. Project Overview

### 1.1 Purpose

The Mini Workshop Booking System is a web application that allows training centers to manage workshops and accept online registrations.

The system provides three user roles:

- Admin
- Instructor
- Visitor

Visitors can browse available workshops and register without creating an account. Administrators can manage workshops, while instructors can view their assigned workshops and registered participants.

The system automatically sends a booking confirmation email after successful registration.

### 1.2 Business Problem

Training centers often manage workshop registrations manually using spreadsheets, emails, or phone calls.

This creates difficulties in:

- Managing workshop information.
- Tracking participant registrations.
- Monitoring workshop capacity.
- Sharing participant information with instructors.
- Confirming registrations with visitors.

### 1.3 Business Objectives

The system aims to:

1. Allow administrators to manage workshops efficiently.
2. Allow visitors to browse available workshops.
3. Allow visitors to register online without creating an account.
4. Allow instructors to view their assigned workshops and participants.
5. Automatically send booking confirmation emails after successful registration.

---

## 2. User Roles and Permissions

The system supports three user roles.

### 2.1 Admin

Admin is responsible for managing the system.

Permissions:

- Log in to the system.
- Create workshops.
- Edit workshops.
- Delete workshops.
- Assign instructors to workshops.
- View all workshops.
- View all registered participants.

### 2.2 Instructor

Instructor is responsible for assigned workshops.

Permissions:

- Log in to the system.
- View assigned workshops.
- View participants registered for assigned workshops.

Restrictions:

- Cannot create workshops.
- Cannot edit workshops.
- Cannot delete workshops.
- Cannot view workshops assigned to other instructors.
- Cannot manage participant registrations.

### 2.3 Visitor

Visitor is a public user who wants to participate in a workshop.

Permissions:

- Browse available workshops.
- View workshop details.
- Register for a workshop.
- Receive booking confirmation emails.

Restrictions:

- Does not need an account.
- Cannot access Admin or Instructor functions.

---

## 3. Functional Requirements

The system contains five core features.

### FR-01: Create and Manage Workshops

Admin can:

- Create a new workshop.
- Edit existing workshop information.
- Delete a workshop.
- Assign an instructor.
- Set maximum participant capacity.
- Set workshop date and time.
- Set workshop location and description.

Workshop information:

| Field                | Required |
| -------------------- | -------- |
| Title                | Yes      |
| Description          | Yes      |
| Date                 | Yes      |
| Start Time           | Yes      |
| End Time             | Yes      |
| Location             | Yes      |
| Maximum Participants | Yes      |
| Instructor           | Yes      |
| Status               | Yes      |

Workshop statuses:

- Open
- Full
- Completed

### FR-02: Display Available Workshops

Visitors can:

- View available workshops.
- View workshop details.
- See workshop date and time.
- See workshop location.
- See assigned instructor.
- See available participant capacity.
- Identify workshops that are Full or Completed.

Only Open workshops that have not started and have available capacity are eligible for new registrations.

### FR-03: Register for a Workshop

Visitors can register without creating an account.

Registration information:

| Field        | Required |
| ------------ | -------- |
| Full Name    | Yes      |
| Email        | Yes      |
| Phone Number | No       |
| Workshop     | Yes      |

Registration process:

1. Validate registration information.
2. Verify that the workshop exists.
3. Check workshop availability.
4. Check for duplicate registration.
5. Verify remaining capacity.
6. Save the registration.
7. Update workshop capacity and status.
8. Trigger booking confirmation email.

### FR-04: View Registered Participants

Admin can:

- View participants for all workshops.
- View participant information.
- View registration dates.

Instructor can:

- View only assigned workshops.
- View participants for assigned workshops.
- View participant names, emails, phone numbers, and registration dates.

Instructor cannot manage workshops or modify registrations.

### FR-05: Send Booking Confirmation Email

The system must trigger a booking confirmation email after successful registration.

The confirmation email contains:

- Participant name.
- Workshop title.
- Workshop date and time.
- Workshop location.
- Registration confirmation message.

Email rules:

- Send confirmation only after successful registration.
- Do not send confirmation when registration fails.
- Use a test SMTP environment during development.
- Do not expose SMTP credentials in source code.

---

## 4. Business Rules

| ID    | Business Rule                                                                           |
| ----- | --------------------------------------------------------------------------------------- |
| BR-01 | Workshop title is mandatory.                                                            |
| BR-02 | Workshop description is mandatory.                                                      |
| BR-03 | Workshop date and time are mandatory.                                                   |
| BR-04 | Maximum participant capacity must be greater than zero.                                 |
| BR-05 | Every workshop must have an assigned instructor.                                        |
| BR-06 | Registration requires participant name and valid email.                                 |
| BR-07 | A participant cannot register twice for the same workshop using the same email address. |
| BR-08 | A workshop cannot accept registrations when full.                                       |
| BR-09 | A workshop cannot accept registrations after it has started.                            |
| BR-10 | When capacity is reached, workshop status automatically becomes Full.                   |
| BR-11 | Registration must be rejected when no capacity remains.                                 |
| BR-12 | Instructors can only view their assigned workshops and participants.                    |
| BR-13 | Admin can manage all workshops and view all participants.                               |
| BR-14 | Confirmation email is triggered only after successful registration.                     |
| BR-15 | Deleting a workshop with existing registrations must be prevented.                      |

Additional capacity rule:

The application must prevent registrations from exceeding workshop capacity, including when multiple visitors attempt registration at approximately the same time.

---

## 5. User Stories

### Admin Stories

**US-001: Create Workshop**

As an Admin, I want to create a workshop so that visitors can discover and register for it.

**US-002: Edit Workshop**

As an Admin, I want to edit workshop information so that I can update incorrect or outdated details.

**US-003: Delete Workshop**

As an Admin, I want to delete a workshop that has no registrations so that unnecessary workshops can be removed.

**US-004: Assign Instructor**

As an Admin, I want to assign an instructor to a workshop so that the instructor can view its participant list.

### Visitor Stories

**US-005: Browse Workshops**

As a Visitor, I want to browse available workshops so that I can choose one that interests me.

**US-006: Register for Workshop**

As a Visitor, I want to register for a workshop without creating an account so that registration is simple.

**US-007: Receive Confirmation**

As a Visitor, I want to receive a confirmation email so that I know my registration was successful.

### Instructor Stories

**US-008: View Assigned Workshops**

As an Instructor, I want to view my assigned workshops so that I know which workshops I am responsible for.

**US-009: View Participants**

As an Instructor, I want to view participants registered for my workshops so that I can prepare for the training.

---

## 6. Acceptance Criteria

### AC-01: Workshop Creation

- Given an authenticated Admin.
- When the Admin enters valid workshop information.
- Then the system creates the workshop successfully.
- And the workshop appears in the workshop listing.

### AC-02: Workshop Validation

- Given an authenticated Admin.
- When the Admin submits a workshop with missing required information.
- Then the system rejects the request.
- And displays appropriate validation errors.

### AC-03: Workshop Browsing

- Given a Visitor opens the workshop listing.
- When available workshops exist.
- Then the system displays their details and available capacity.

### AC-04: Successful Registration

- Given an Open workshop with available capacity.
- When a Visitor submits valid registration information.
- Then the system saves the registration.
- And updates the remaining capacity.
- And triggers a confirmation email.

### AC-05: Duplicate Registration

- Given a Visitor has already registered for a workshop.
- When the same email attempts to register again for that workshop.
- Then the system rejects the duplicate registration.

### AC-06: Full Workshop

- Given a workshop has reached maximum capacity.
- When another Visitor attempts registration.
- Then the system rejects the registration.
- And the workshop status remains Full.

### AC-07: Instructor Access

- Given an authenticated Instructor.
- When the Instructor views their workshops.
- Then the system displays only assigned workshops.

### AC-08: Participant Access

- Given an authenticated Instructor.
- When the Instructor views participants.
- Then the system displays participants from assigned workshops only.

### AC-09: Confirmation Email

- Given a successful registration.
- When the registration is saved.
- Then a confirmation email is triggered to the visitor's email address.

### AC-10: Workshop Deletion

- Given a workshop has existing registrations.
- When an Admin attempts to delete it.
- Then the system prevents deletion and displays an appropriate message.

---

## 7. Non-Functional Requirements

| ID     | Requirement                                                              |
| ------ | ------------------------------------------------------------------------ |
| NFR-01 | Application must be accessible through a modern web browser.             |
| NFR-02 | Application must validate user input.                                    |
| NFR-03 | Admin and Instructor pages must require authentication.                  |
| NFR-04 | Passwords must be stored securely using password hashing.                |
| NFR-05 | Application must prevent unauthorized access to instructor data.         |
| NFR-06 | Application must handle invalid requests with meaningful error messages. |
| NFR-07 | Application must support automated backend unit testing.                 |
| NFR-08 | Database credentials must not be committed to GitHub.                    |
| NFR-09 | Registration must not exceed workshop capacity.                          |

---

## 8. Project Scope

### Included in Version 1

- Workshop CRUD.
- Public workshop listing.
- Visitor registration.
- Admin and Instructor login.
- Instructor assignment.
- Participant listing.
- Booking confirmation email.
- Unit testing.

### Excluded from Version 1

- Online payments.
- SMS notifications.
- QR-code tickets.
- Attendance scanning.
- Workshop certificates.
- Waitlists.
- Password reset.
- Multiple organizations or training centers.
- Advanced analytics.

---

## 9. Technology Stack

| Layer                   | Technology              |
| ----------------------- | ----------------------- |
| Backend                 | Java 21                 |
| Framework               | Spring Boot             |
| Build Tool              | Maven                   |
| Frontend                | React                   |
| Database                | PostgreSQL              |
| Communication           | REST API                |
| Backend Testing         | JUnit 5 and Mockito     |
| Email                   | SMTP                    |
| Version Control         | Git and GitHub          |
| Development Methodology | Spec-Driven Development |

---

## 10. Document Approval

| Item             | Value                                      |
| ---------------- | ------------------------------------------ |
| Document Version | 1.0                                        |
| Approval Status  | Approved by Product Owner                  |
| Project          | Mini Workshop Booking System               |
| Next Stage       | Software Architecture and Technical Design |

This document represents the approved product requirements for Version 1.0.

Any changes to these requirements should be reviewed and approved before implementation.
