# US-09 — Comment on a video

**Target User Story / Task ID:** US-09 — Comment on a video

**Description:**
Authenticated users can add a non-empty comment to a video from the detail page. Visitor users are redirected to the login flow instead of being able to submit comments. Comments must persist in a real database and appear under the video after submission.

## Architecture and security constraints

- Follow the existing Controller -> Service -> Repository layering.
- Keep controllers returning DTOs, not JPA entities.
- Persist comments in PostgreSQL using JPA; do not rely on in-memory persistence.
- Protect the comment write endpoint so only authenticated users can submit comments.
- Preserve the existing CSRF / session / CORS behavior used by registration and login.
- Keep media and video detail reads public; comments are retrieved as part of the video detail response.
- Frontend implementation must use React Testing Library queries by role and mock API calls in tests.

## Expected acceptance criteria

- Given an authenticated user on a video detail page, when they submit a non-empty comment, then it is persisted and displayed under the video.
- Given an unauthenticated user, when they try to comment, then they are prompted to log in.
- Comment creation rejects empty or whitespace-only content.
- Comments are returned with the author email and timestamp information in the video detail response.

## Validation tests

- Backend HTTP tests for authenticated POST /api/videos/{id}/comments and unauthorized access.
- Backend service tests verifying persistence and validation behavior.
- Frontend detail-page tests covering comment form rendering, successful submit and prompt-for-login states.
- Full targeted backend/frontend test run before closing the task.
