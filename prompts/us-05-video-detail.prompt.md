# Prompt: US-05 - Play a video and view its details

## Context & Objective
Implement US-05 for ProTube: open a video from the catalog, play it on a detail page, and show its title, description and comments.

## Backend Requirements
- Add a REST endpoint that returns a single video detail by id.
- Follow Controller -> Service -> Repository layering and expose DTOs only.
- Reuse the existing PostgreSQL-backed video catalog and the configured video store directory.
- Return 404 when the video id does not exist or the media/metadata files are unavailable.
- Read description and comments from the imported video metadata.

## Frontend Requirements
- Make catalog items open a video detail route.
- Render a video player, title, description and comments on the detail page.
- Show a not-found state for missing or unavailable videos instead of a broken player.

## Constraints
- Preserve the current authentication, session and catalog behavior.
- Keep media URLs based on `src/utils/Env.ts`.
- Use the existing `ENV_PROTUBE_STORE_DIR` storage convention with a trailing separator.

## Validation
- Backend controller and service tests for success and 404 cases.
- Frontend tests for catalog navigation, detail rendering and not-found state.
- Run backend verification and frontend tests after implementation.
