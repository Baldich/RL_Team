# US-04 — Browse Video List

**Approved by:** Requesting user authorized implementation in this conversation. No separate team approval is asserted.
**Approved on:** 2026-10-14
**Used for:** US-04 architecture, backend/frontend implementation, tests and documentation.

## Prompt

Implement US-04 — Browse Video List following the reviewed implementation plan.

1. Create a persistent Video entity and repository using the existing PostgreSQL setup.
2. Import videoGrabber metadata from the configured video storage directory into PostgreSQL. Prevent duplicates and handle invalid or missing files safely.
3. Implement GET /api/videos with paginated video summaries containing ID, title and thumbnail path.
4. Replace the placeholder frontend video list with a responsive grid displaying real video thumbnails and titles.
5. Implement Previous/Next pagination, loading, empty and error states.
6. Ensure browsing videos works for authenticated and unauthenticated users.
7. Preserve all US-01 registration and US-02 login functionality.
8. Add backend and frontend tests covering the US-04 acceptance criteria.
9. Use the existing architecture and dependencies. Do not implement playback, upload or search functionality.
10. Preserve the approved AI prompt according to AGENTS.md.

Run backend Maven tests and coverage verification; run frontend tests, lint and build.
Report executed results, coverage and remaining issues. Explain importing existing videos and manual catalog validation.
Work only on feature/us-04-video-list. Do not change unrelated functionality, commit or push. Never claim unexecuted tests passed.

## Architecture and security constraints

- Controller -> Service -> Repository; controllers expose DTOs, never JPA entities.
- NFR-01: PostgreSQL persistence using existing JPA configuration; no H2/in-memory application catalog.
- Relative media filenames only; storage root comes from ENV_PROTUBE_STORE_DIR, including its trailing separator.
- Generated database IDs are separate from unique grabber source IDs, including source ID zero.
- Import only complete JSON/WEBP/MP4 sets; do not follow individual file symlinks or delete persisted data.
- Public GET catalog and media access; preserve session, CSRF, CORS, registration and login behavior.
- Explicit page DTO; default page 0, size 12; deterministic ID ordering; size bounded to 100.
- Frontend URLs use src/utils/Env.ts; tests mock requests and query primarily by role.

## Acceptance criteria and validation

- Visitors and signed-in users see available grabber videos with titles and thumbnails.
- Previous/Next loads additional pages without errors or repeated entries.
- Import handles repeated startup, malformed metadata, missing media and unavailable directories safely.
- PostgreSQL integration tests prove persistence, uniqueness and pagination.
- HTTP tests cover public/authenticated access, page validation and DTO serialization.
- UI/hook tests cover loading, success, empty, error/retry, pagination and outdated requests.
- Preserve authentication regression tests and run Maven verify, frontend tests, lint and build.
- Coverage: frontend >=75% branches/functions/lines/statements; backend >=75% overall instructions/lines/methods and >=50% lines per class (existing exclusions).

## Planning prompt preserved

Analyze and plan US-04 without modifying files: read REQUIREMENT.md, README.md and AGENTS.md; inspect backend/frontend architecture and existing entities, repositories, services, endpoints and components; identify existing functionality and gaps; propose the simplest implementation, affected files, acceptance tests, authentication dependencies and AI policy approvals/documentation. Do not implement, commit, push or change unrelated functionality.

## Review

Team review of AI contributions remains required before merge. Commit messages must identify AI use, US-04 and this prompt. No commit or push is authorized in this task.

## Validation follow-up

Catalog integration exercises real registration, login, failed login and logout to prove browsing stays public and works with authenticated sessions. Existing LoginForm.tsx and auth.test.ts receive formatting-only fixes required for full-project lint; no authentication behavior changes.
