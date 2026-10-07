# ProTube

Video-on-demand platform. Monorepo structure:
- `backend/`: Spring Boot 3.3 (Java 21) REST API via Maven.
- `frontend/`: React 19 + TypeScript SPA via Vite.
- `tooling/videoGrabber/`: Python sample video ingestion script.

See [README.md](./README.md) for project rules and
[REQUIREMENT.md](./REQUIREMENT.md) for MVP specs.

## Essential Commands

### Backend (`backend/`)
- Test, build & coverage: `mvn clean verify`
- Run dev server: `mvn spring-boot:run`

### Frontend (`frontend/`)
- Install dependencies: `npm install`
- Dev server: `npm run dev` (port 5173)
- Run tests & coverage: `npm run test`
- Lint & format: `npm run lint` / `npm run format`

### Video Grabber (`tooling/videoGrabber/`)
- Requires Python 3, `yt-dlp`, and `ffmpeg`.
- Generates the `.mp4`, `.webp`, and `.json` files used by the backend.
- On Windows, use `python` to run the script.

## Environment & Architecture Rules

- **Store Path:** Backend requires `ENV_PROTUBE_STORE_DIR` pointing to
  the absolute path of the local video store. The path must end with `/`
  or `\`. Never hardcode local storage paths.
- **Backend Layers:** Follow `Controller` -> `Service` -> `Repository`
  layering.
- **DTOs:** Controllers receive and return DTOs, never JPA entities
  directly.
- **Frontend Tests:** Use React Testing Library and query by role.
  Mock API calls; never hit real backend services in unit tests.
- **Frontend Env:** Frontend environment configuration is read through
  `src/utils/Env.ts` using `VITE_API_DOMAIN` and `VITE_MEDIA_DOMAIN`.
- **Requirements:** Check `REQUIREMENT.md` before implementing a feature
  for its acceptance criteria and priority.

## AI Policy

- Follow the AI usage rules defined in `README.md` and `REQUIREMENT.md`.
- Prompts used for AI-assisted code, architecture, or other project work
  must be preserved in `prompts/`.
- Reuse an existing approved prompt when applicable.
- AI-generated changes must remain traceable and be reviewed by the team.

## Workflow & Definition of Done (DoD)

- **Branches:** `main` is protected. Work in
  `feature/<ticket>-short-name`, `fix/...`, or `chore/...` branches.
- **Commits:** Use Conventional Commits (`feat:`, `fix:`, `chore:`, etc.).
- **Pull Requests:** All contributions go through a PR.
- **Merging:** CI must pass and at least one human teammate must approve
  the PR before squash merge.
- **DoD:**
    - Code builds without errors.
    - Tests pass locally and in CI.
    - Frontend coverage: at least 75% for branches, functions, lines and statements.
    - Backend coverage: at least 75% overall for instructions, lines and methods;
      at least 50% line coverage per class (JaCoCo exclusions apply).
    - Lint checks pass.
    - Automated/Copilot review comments are resolved.