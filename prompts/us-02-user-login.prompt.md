# Prompt: US-02 - User Login

## Context and objective
Implement the complete ProTube login flow for registered users using the existing Spring Boot and React architecture.

## Constraints
- Preserve PostgreSQL-only persistence (NFR-01); never add H2 or in-memory user storage.
- Verify passwords with the existing PBKDF2 `PasswordEncoder` against `app_users`.
- Keep the Controller -> Service -> Repository dependency direction and expose DTOs only.
- Protect state-changing requests with the existing session-backed CSRF token.
- Rotate the session ID after successful authentication and use HttpOnly/SameSite=Lax cookies.
- Return the same generic `401 Unauthorized` message for unknown users and wrong passwords.

## Acceptance criteria
- Correct credentials create an authenticated session and return the safe user DTO.
- Invalid credentials return `401` and do not create a session.
- Login and logout are available from `/login` and the authenticated navigation state.
- Logout invalidates the session and requires CSRF protection.
- Network and credential errors are shown inline in the login form.

## Validation
- Backend controller tests cover successful login, invalid credentials, CSRF and logout.
- Frontend API tests mock login/logout endpoints and verify CSRF credentials.
- Run `mvn clean verify` from `backend/` and `npm test` from `frontend/`.
