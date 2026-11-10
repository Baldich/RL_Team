# NFR-03: Containerized delivery

## Task

Update `compose.yaml` so a clean machine with Docker can start the complete
ProTube application without manually configuring database or application
environment variables.

## Constraints

- Keep the required Controller -> Service -> Repository architecture unchanged.
- Use PostgreSQL 16 with persistent Docker storage.
- Provide safe local defaults for database credentials and the media store while
  allowing environment variables to override them.
- Run the backend with Java 21/Maven and the production Maven profile so the
  frontend is bundled and served by the backend.
- Build small project images that provide FFmpeg/FFprobe for backend uploads and
  the videoGrabber tool, without requiring host-side Java, Maven, Python, or
  FFmpeg installations.
- Keep media storage persistent and writable because authenticated video uploads
  are supported.
- Preserve the optional videoGrabber service behind the `tools` profile.
- Do not commit real credentials or secrets.

## Acceptance criteria and validation

- `docker compose up` starts PostgreSQL and the backend without requiring the
  professor to define `ENV_PROTUBE_DB`, `ENV_PROTUBE_DB_USER`,
  `ENV_PROTUBE_DB_PASS`, or `ENV_PROTUBE_STORE_DIR`.
- The backend connects to the Compose database service and is available at
  `http://localhost:8080`.
- The production backend serves the bundled SPA at `/` while preserving
  `/media/**` for files from the configured media store.
- The production profile imports valid videoGrabber metadata/media triples from
  the mounted store at startup, while preserving manually uploaded videos.
- Imported tags longer than the existing 50-character database limit are
  skipped with a warning so one malformed legacy JSON cannot prevent startup.
- PostgreSQL data, Maven dependencies, frontend dependencies, backend output,
  and media files persist across container restarts.
- `docker compose config` succeeds with no unresolved required variables.
