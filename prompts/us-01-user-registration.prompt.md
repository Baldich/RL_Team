# Prompt: US-01 - User Registration

## Context & Objective
Implement User Registration for ProTube following Spring Boot and React best practices.
The implementation must satisfy NFR-01 (PostgreSQL persistence) and NFR-03/NFR-04 security guidelines.

## Backend Requirements
- Create registration endpoint `/api/auth/register` handling email and password.
- Enforce minimum 8 characters for passwords and valid email format.
- Store hashed passwords using PBKDF2 (`Pbkdf2PasswordEncoder`).
- Implement CSRF protection and session rotation upon registration.
- Provide `/api/auth/session` and `/api/auth/csrf` endpoints.
- Map duplicate email conflicts to HTTP 409 status code.

## Frontend Requirements
- Build `RegistrationForm` component with inline client-side validation.
- Display server-side errors on duplicate emails and invalid fields.
- Persist session state and display authenticated user email on the top header.