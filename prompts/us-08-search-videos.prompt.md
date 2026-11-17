# Prompt: US-08 - Search videos

## Context and objective
Implement US-08 so users can search the public video catalog by title and description.

## Constraints
- Keep the existing Controller -> Service -> Repository layering and paginated DTO response.
- Use PostgreSQL full-text search with `simple` tokenization and relevance ranking, followed by a stable ID tie-breaker.
- Keep empty queries equivalent to the existing catalog and return an empty page for no matches.
- Keep the endpoint public and preserve existing pagination and media URL behavior.
- Use an accessible submitted-search form in React and mock all API calls in frontend tests.

## Acceptance criteria and validation
- Matching title or description returns paginated results ranked by relevance.
- A query with no matches shows a non-error empty state.
- An empty query does not error and restores the normal catalog.
- Validate query length and preserve retry, pagination, and stale-request handling.
- Run backend verification and frontend build, tests, type-check, and lint.
