# US-06 — Upload a Video

## Request and scope
Implement the complete Sprint 2 US-06 as requested by the user: backend multipart
upload, React upload page, catalog/player integration, tests and documentation.
Work only on feature/us-06-upload-video; do not commit, push, open a PR, or touch
local IntelliJ configuration or unrelated untracked files.

## Constraints
Use Java 21/Spring Boot 3.3, PostgreSQL (NFR-01), Controller → Service → Repository,
DTO boundaries, existing sessions and CSRF. Accept MP4/H.264 up to 100 MiB with
trimmed required title (255) and description (5000). Inspect content, use bounded
FFmpeg processes, generate thumbnails, stage privately, use UUID paths, and clean
up on failure including database commit failure. Preserve imported video sidecars,
catalog ordering, comments, and existing US-07 ownership/metadata editing.

## Acceptance and verification
Authenticated upload returns 201 and appears in catalog and player; anonymous page
access redirects to login after session initialization. Failed uploads show clear
errors and publish no incomplete records. Test validation, authentication/CSRF,
processing/storage failures, rollback cleanup, ownership and existing playback.
Run Maven verification, frontend tests/build/lint; retain 75% coverage gates and
backend 50% per-class line coverage. Report actual results and limitations.

## Traceability
Preserved from the user's explicit implementation instruction in this session.
Team review/approval remains part of the repository contribution process; no team
approval or commit is claimed here. This file must accompany a future reviewed PR.
