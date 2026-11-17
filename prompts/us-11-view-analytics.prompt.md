# US-11 - View analytics for my videos

## Request and scope

Implement US-11: an authenticated user can view persistent analytics for their own uploaded videos,
including views, comments, and likes, with an empty state when no videos have been uploaded.

## Architectural and security constraints

- Follow the existing Controller -> Service -> Repository layering and expose DTOs rather than JPA entities.
- Keep analytics scoped to the authenticated session user; unauthenticated requests receive HTTP 401.
- Persist view counts in PostgreSQL on the video entity and derive comments and likes from their persistent repositories.
- Keep media storage configured through `ENV_PROTUBE_STORE_DIR`; do not add local paths.
- Use the existing React/TypeScript API and environment configuration, credentialed requests, accessible role/table markup,
  and mocked API calls in frontend tests.

## Acceptance criteria and validation

- An authenticated user sees aggregate and per-video views, comments, and likes for only their own videos.
- A user with no uploaded videos sees an empty state instead of an error.
- Opening a video records a persistent view and analytics reflects it.
- The analytics route is available from the authenticated navigation and video titles open their detail page.
- Validate with backend `mvn clean verify`, frontend tests/build/lint, and `git diff --check`.
