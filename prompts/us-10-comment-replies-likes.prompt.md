# US-10 — Reply to and like comments

## Request and scope

Implement US-10 in English as explicitly requested by the user on 2026-11-16:
authenticated replies, likes and unlikes, persistence, UI, tests and documentation.
Work on feature/us-10-comment-replies-likes. Preserve existing functionality and
local configuration. Team review and CI remain required before merge.

## Architecture and security constraints

- Java 21 / Spring Boot 3.3, React / TypeScript, PostgreSQL only (NFR-01).
- Follow Controller -> Service -> Repository and expose DTOs only.
- Derive the acting user from the session; require authentication and CSRF for
  replies, likes and unlikes. Extend credentialed CORS to DELETE.
- Support both JSON/videoGrabber comments and locally posted comments/replies.
  Use video-scoped opaque references: imported-<original-array-index> and
  local-<database-id>. Never rewrite the source JSON or manufacture user accounts.
- Keep existing comments as roots by adding a nullable parent reference. Store
  likes separately with a unique (video, comment reference, user) constraint.
  PUT/DELETE likes are idempotent, including concurrent repeated PUT requests.
  Preserve imported/historical like counts as a baseline.
- Validate that targets belong to the requested video before writing. Reject
  empty/oversized text and missing targets. Bound reply depth to eight levels.
- Public detail reads include nested replies and the session user's like state;
  send session credentials and prevent caching of personalized detail responses.
- Keep media paths in ENV_PROTUBE_STORE_DIR and frontend URLs in Env.ts.
- Show accessible English controls, pending/error states, login prompts, and
  confirmed server counts. Preserve drafts on failure and prevent double submits.
  Updating a discussion must not restart playback or refetch the whole detail.

## Acceptance criteria and validation

- A signed-in user can reply to an imported/local comment or reply; the reply is
  stored in PostgreSQL and remains nested after reload.
- Like increments the count and sets the user's state; unlike removes only that
  user's like. Repeated requests cannot duplicate likes or reduce baseline counts.
- Anonymous reads remain available, while anonymous or CSRF-less writes fail.
- Test cross-video targeting, independent user states, persistence, concurrent
  duplicate likes, existing data compatibility, source JSON preservation, and UI
  success/error/session/navigation cases with mocked APIs and role queries.
- Run mvn clean verify (real PostgreSQL Testcontainers and FFmpeg), frontend Jest
  with coverage, lint, build, explicit app TypeScript checking and git diff --check.
  Retain existing coverage gates and report actual outcomes.

The requesting user authorized implementation. No separate team approval or CI
result is asserted. Preserve this prompt alongside the implementation in its PR.

## Executed validation — 2026-11-16

- Backend: `mvn -B clean verify` with Java 21, real PostgreSQL Testcontainers and
  FFmpeg: 106 tests passed, zero failures/errors/skips; build and JaCoCo gates
  passed. Coverage: instructions 96.47%, lines 97.43%, methods 97.94%.
- Frontend: Jest `--watchAll=false --ci --runInBand`: 112 tests passed in 20
  suites. Coverage: statements 91.47%, branches 89.01%, functions 86.51%,
  lines 91.54%; all existing gates passed.
- Frontend lint, production build, explicit app TypeScript checking and
  `git diff --check` passed.
- Regression tests verify the player stays mounted during replies/likes and
  private drafts disappear on logout. Integration tests verify independent user
  state, nested persistence, unchanged source JSON and concurrent duplicate PUTs.
- Docker Desktop was initially stopped and was started for the integration run.
  A JPA relationship-path query issue found by integration tests was corrected
  before the final successful clean verification.
- Manual browser playback, remote CI and human PR review have not been run.
  No commit, push or merge was performed.
