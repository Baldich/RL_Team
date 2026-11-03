# Prompt: US-07 - Edit video metadata

## Context and objective
Implement US-07 so an authenticated owner can persist and edit a video's description, category, and tags.

## Constraints
- Keep the Controller -> Service -> Repository layering and DTO-only HTTP boundaries.
- Persist editable metadata and ownership in PostgreSQL-backed JPA entities.
- Require session authentication and CSRF for updates; deny non-owners with HTTP 403.
- Preserve `ENV_PROTUBE_STORE_DIR` for media storage and keep existing public detail behavior.
- Follow the existing React accessibility and mocked-API testing conventions.

## Acceptance criteria and validation
- An authenticated owner can submit description, category, and tags through `PUT /api/videos/{id}` and see the saved values on the detail page.
- Anonymous users cannot update videos, and users who do not own a video receive 403.
- Validate metadata limits and return the project's standard field-error response.
- Run backend Maven verification and frontend tests/lint.
