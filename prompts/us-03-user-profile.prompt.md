
# US-03 - View and edit profile

## Request and scope

Implement US-03 on the existing feature/us-03-edit-profile branch. A signed-in user can view and edit their
avatar, description and public profile fields. Any visitor can view another user's public information without
edit controls. The user explicitly authorized implementation; team approval before merge remains required.
Do not implement the other Sprint 2 or backlog stories. Do not commit, push or merge.

## Architecture and security constraints

- Follow Controller -> Service -> Repository and expose DTOs only.
- NFR-01: persist profile fields on app_users in PostgreSQL using existing JPA schema-update configuration.
  Nullable new columns preserve existing accounts; unset profile fields are returned as empty strings.
- Public fields: displayName (maximum 80 characters), avatarUrl (maximum 2048), description (maximum 1000).
  All are optional empty strings; trim values. Avatar is an HTTP/HTTPS URL, with an initials placeholder when unset.
  Avatar file upload is outside this implementation's scope. Never expose email, password hashes or session data publicly.
- GET /api/users/{id}/profile is public. GET/PUT /api/profile require authentication.
  Resolve the edited account exclusively from the existing session principal, with CSRF protection for PUT.
  No endpoint allows a caller to submit the target account ID for editing.
- Reuse existing session cookies and extend credentialed CORS to PUT only for the configured frontend origin.
- Frontend uses Env.ts; /profile opens the owner view, /users/{id} the public view.
  Render loading, not-found, error/retry, sign-in-required, field validation and saved states.
  Show edit controls only for the owner and update the visible profile after a successful save.
- Preserve registration, login/logout, catalog and playback, and the pre-existing local IDE configuration change.

## Acceptance criteria and validation

- An authenticated user sees their current avatar, display name and description.
- Saving valid changes persists them in PostgreSQL and immediately updates the profile display.
- Anonymous visitors and other users see only public profile fields with no edit controls.
- Tests cover DTO privacy, invalid URLs and field lengths, unauthenticated access, CSRF, ownership,
  persistence after reloading, missing profiles, defaults for existing users, UI states and auth regressions.
- Use Mockito/MockMvc and PostgreSQL Testcontainers for backend tests; React Testing Library with mocked APIs
  and role queries for frontend tests. Do not use H2 or real backend calls in frontend unit tests.
- Run backend mvn clean verify; frontend Jest with coverage, lint and build. Report actual results and blockers.
  Preserve the existing coverage thresholds and document manual checks in DEVELOPMENT.md.

## Executed validation - 2026-10-21

- Backend: mvn clean verify using local Corretto 21 and PostgreSQL Testcontainers: 64 tests passed,
  zero failures/errors/skips. JaCoCo gates passed: instructions 98.12%, lines 97.16%, methods 100%.
- Frontend: Jest --watchAll=false --ci --runInBand: 79 tests passed in 14 suites. Coverage gates passed:
  statements 94%, branches 90.62%, functions 92.4%, lines 94.15%.
- npm run lint: passed without warnings. npm run build: passed.
- Explicit tsc --project tsconfig.app.json --noEmit: passed, including src (the existing root config
  primarily includes tooling). No unrelated TypeScript configuration changes were made.
- git diff --check: passed. Existing local .run configuration was preserved. No commit or push performed.
- Docker/Maven and Vite build needed sandbox escalation for Docker/cache access and esbuild directory reads.
  CI and manual browser checks have not been run in this task.
