# Development guide

## US-06: authenticated video uploads

The backend host must provide **FFmpeg and FFprobe** on PATH, including the
H.264 decoder, libx264 (test fixture generation), and libwebp encoder. These are
now backend runtime dependencies, in addition to their use by videoGrabber.
Optionally set `ENV_PROTUBE_FFMPEG` and `ENV_PROTUBE_FFPROBE` to executable paths.
Processes use argument lists without a shell, a 120-second timeout per process,
and at most two concurrent processing pipelines per backend instance. CI installs
FFmpeg before backend tests. Backend runtime tests require both commands.

### Contract and storage

- `POST /api/videos` accepts multipart parts `file` (binary) and `metadata`
  (`application/json`, containing `title` and `description`). The browser sends
  the session cookie and the header returned by `GET /api/auth/csrf`.
- Required metadata is trimmed: title 1–255 characters, description 1–5000.
  Ownership always comes from the session. The existing US-07 editor is reused.
- Supported content: MP4 with H.264, 8-bit 4:2:0 video, and AAC audio or no audio.
  Actual media is probed and decoded; extension/MIME alone is insufficient.
- Maximum file size is **100 MiB (104857600 bytes)**, displayed as 100 MB in the
  interface. The multipart request limit is 101 MiB to allow metadata/overhead.
- Success returns `201`, a `/api/videos/{id}` Location, and the existing detail
  DTO shape. Errors include 400 (fields/parts), 401 (session), 403 (CSRF), 413
  (size), 415 (unsupported media), 422 (corrupt/timed-out processing), 503 (busy),
  and a sanitized 500 (storage/tool/database failure).
- Servlet multipart files and processing files use the OS temporary directory,
  outside the public store. Do not configure the OS or servlet temporary directory
  inside `ENV_PROTUBE_STORE_DIR`. Ensure it has sufficient free space and private
  permissions for the backend account.
- Completed media uses UUID `.mp4` and `.webp` names in the existing configured
  store. Only a completed file pair is recorded in PostgreSQL. Failed copying,
  processing, persistence, or transaction commit triggers cleanup. Cleanup errors
  are logged without exposing paths. Abrupt process termination or disk/permission
  failures can leave unreferenced assets: operators should reconcile unreferenced
  UUID pairs and abandoned OS `protube-upload-*` directories while the backend is
  stopped. No automatic deletion of existing content is performed.
- Keep the PostgreSQL volume **and the media store** across restarts. A future
  backend container must mount a persistent writable store and include FFmpeg.
  The existing Compose file still provides PostgreSQL only.
- Uploaded rows have `sourceId = null`; details use database metadata/comments
  without a JSON sidecar. Imported numeric videoGrabber triples remain supported.
- Catalog order stays ascending by database ID. New uploads can appear on later
  pages; successful upload navigates directly to its detail page.
- A lost success response can leave a complete published video. The UI advises
  checking the catalog before retrying; uploads are not automatically retried.

### Local manual verification

1. Install Java 21, Maven, Node 22, FFmpeg/FFprobe, and start PostgreSQL as described
   below. Set `ENV_PROTUBE_STORE_DIR` to a persistent writable absolute directory
   ending with `/` or `\`, and set the existing database environment variables.
2. Run `mvn spring-boot:run` in `backend/` and `npm run dev` in `frontend/`.
3. Open `http://localhost:5173/upload` while signed out: wait for session loading
   and verify redirection to `/login`. Register or log in, then click **Upload Video**.
4. Select an MP4/H.264 file, enter a title and description, and upload. Verify the
   pending state prevents resubmission, then the new detail page opens and plays.
5. Return to the catalog and paginate to the new entry. Verify its title and
   thumbnail. Open it in a signed-out browser and verify playback and seeking.
6. As the owner, edit metadata using the existing editor and post a comment.
   Reload and verify both persist. As another user, verify no owner edit controls.
7. Restart the backend using the same database and store. Verify the upload remains
   listed, plays, and retains its edited description and comment. Also play an
   imported videoGrabber video to check compatibility.
8. Try blank metadata, a WebM, corrupt bytes renamed `.mp4`, and a file larger
   than 100 MiB. Verify clear errors and no new catalog entries or partial files.
   Disconnect the network or make a dedicated test store unwritable to exercise
   failures; restore permissions/connectivity afterward. Do not use live data for
   destructive failure simulations.
9. Reload `/upload` in the production build to verify backend SPA forwarding.

### Automated verification

Run `mvn clean verify` (Java 21, Docker for PostgreSQL Testcontainers, FFmpeg and
FFprobe), and frontend `npm run test -- --watchAll=false --ci --runInBand`,
`npm run build`, and `npm run lint`.

`VideoUploadIntegrationTest` covers real FFmpeg upload → PostgreSQL → public
catalog/details → media range requests → existing owner editing and comments.
Unit/security tests cover failures, rollback cleanup and metadata/authentication.
Frontend tests mock APIs and cover form, session initialization, redirect and
success navigation. There is no existing browser E2E runner in this repository;
this change adds server integration coverage without introducing a new browser
framework. Actual browser playback remains a manual check; a full browser E2E
suite is still required to complete the broader NFR-02 scope.

Traceability: `prompts/us-06-upload-video.prompt.md`. Include the prompt and changes
in the eventual team-reviewed contribution; no commit or PR is made by this task.

How to set up, run, test and build ProTube locally. For the assignment rules see [README.md](./README.md), and for
the MVP spec see [REQUIREMENT.md](./REQUIREMENT.md).

## Repository layout

| Folder                   | What it is                                                                 |
|--------------------------|----------------------------------------------------------------------------|
| `backend/`               | Spring Boot 3.3 REST API (Java 21, Maven). Serves `/api/**` and `/media/**` |
| `frontend/`              | React 19 + TypeScript SPA (Vite)                                           |
| `tooling/videoGrabber/`  | Python script that downloads sample videos into the video store folder     |
| `resources/`             | Images for the docs and `video_list.txt` (input for the video grabber)     |
| `.run/`                  | Shared IntelliJ IDEA run configurations                                    |
| `prompts/`               | Approved AI prompts (see the "Usage of AI" section of the README)          |

## Prerequisites

* **Java 21** (JDK; CI uses Amazon Corretto 21).
* **Maven 3.9+**. The Maven wrapper (`mvnw`) is **not** committed; either use your local `mvn`, or generate the
  wrapper once inside `backend/` with `mvn -N wrapper:wrapper` (CI does the equivalent). The commands below use
  `mvn`; swap in `./mvnw` if you generated it.
* **Node.js 22** and npm (CI uses Node 22; the production Maven build downloads Node v22.12.0 on its own).
* **Python 3, `yt-dlp` and `ffmpeg`** — only if you need to generate the sample videos.

## 1. Generate the video store

The backend reads videos from a *store* folder: one `.mp4` / `.webp` / `.json` triple per video, produced by the
video grabber. Do this once before running the backend.

```bash
cd tooling/videoGrabber
pip install -r dependencies.txt
python3 main.py --store=/absolute/path/to/videos --id=10 --videos=../../resources/video_list.txt
```

* `--id` is your group id (used to pick the set of videos).
* `--recreate` wipes and regenerates an existing store folder (without it, the script exits if the folder exists).
* Use `python` instead of `python3` on Windows if that's how Python is installed.

## 2. Environment variables

| Variable                           | Needed for              | Description                                                                                      |
|------------------------------------|-------------------------|--------------------------------------------------------------------------------------------------|
| `ENV_PROTUBE_STORE_DIR`            | always                  | Absolute path of the video store folder. **End it with a path separator** (`/` or `\`) — it is used as a `file:` prefix to serve `/media/**`. |
| `ENV_PROTUBE_GOOGLE_CLIENT_ID`     | Google OAuth login      | Google OAuth client id                                                                           |
| `ENV_PROTUBE_GOOGLE_CLIENT_SECRET` | Google OAuth login      | Google OAuth client secret                                                                       |
| `ENV_PROTUBE_DB_USER` | PostgreSQL | Database user (default postgres); set explicitly for Compose. |
| `ENV_PROTUBE_DB_PASS` | PostgreSQL | Database password; set explicitly for Compose. |
| `ENV_PROTUBE_DB` | PostgreSQL | Database name (default protube). |

Never commit real secrets. Set them in your shell, your OS, or your (local, uncommitted) IDE run configuration.

```bash
# Linux / macOS
export ENV_PROTUBE_STORE_DIR=/home/me/protube/videos/
```

```powershell
# Windows PowerShell (current session)
$env:ENV_PROTUBE_STORE_DIR = "D:\code\videos\"
```

## 3. Development mode

In development the backend and frontend run as two separate processes:

```
Browser ──► Vite dev server (http://localhost:5173) ──fetch──► Spring Boot (http://localhost:8080/api, /media)
```

### Docker Compose production mode

With Docker Desktop installed, the complete application can be started without
setting environment variables:

```bash
docker compose up
```

The bundled frontend and backend are available at
`http://localhost:8080`. Compose defaults the PostgreSQL credentials to
`protube` and uses `./store` for media; set `ENV_PROTUBE_STORE_DIR` only when
using another store. PostgreSQL data and the media store survive container
restarts. The backend and videoGrabber images include FFmpeg/FFprobe. To
generate sample videos, run
`docker compose run --rm videograbber` (the service is in the `tools` profile).
The production profile imports valid numeric videoGrabber triples from the
mounted store at startup. Existing files must include matching `<id>.json`,
`<id>.webp` and `<id>.mp4` files; after generating or copying them, restart the
backend with `docker compose restart backend`.

### Backend

```bash
cd backend
mvn spring-boot:run
```

* Starts on **http://localhost:8080** with the `dev` profile (active by default through the Maven `dev` profile).
* `dev` uses PostgreSQL and imports complete videoGrabber file sets at startup (`pro_tube.load_initial_data=true`).
  Catalog metadata remains available after restart; repeated imports skip existing source IDs.
* From IntelliJ you can instead use the shared **ProtubeBackApplication** run configuration (`.run/`) — edit its
  environment variables to point to your own store folder.
* API CORS allows the configured frontend origin (`ENV_PROTUBE_FRONTEND_ORIGIN`, default http://localhost:5173).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

* Starts on **http://localhost:5173** with hot reload (or use the **frontend [DEV]** IntelliJ run configuration).
* API and media URLs come from `frontend/.env.development`:
  ```
  VITE_API_DOMAIN=http://localhost:8080
  VITE_MEDIA_DOMAIN=http://localhost:8080
  ```
  and are read through `src/utils/Env.ts` (`API_BASE_URL = ${VITE_API_DOMAIN}/api`,
  `MEDIA_BASE_URL = ${VITE_MEDIA_DOMAIN}/media`). To override locally without touching the committed file, create
  `frontend/.env.development.local`.

## 4. Tests, lint and formatting

### Backend

```bash
cd backend
mvn clean verify                          # build + all tests
mvn test -Dtest=ClassName                 # one test class
mvn test -Dtest=ClassName#methodName      # one test method
mvn clean verify -Pcoverage               # tests + JaCoCo report → target/site/jacoco/index.html
```

### Frontend

```bash
cd frontend
npm run test                              # Jest + Testing Library, coverage → coverage/
npx jest path/to/File.test.tsx            # a single test file
npm run lint                              # ESLint
npm run lint-fix                          # ESLint with autofix
npm run format                            # Prettier
```

The current gates require frontend coverage of at least 75% for branches, functions, lines and statements; backend coverage of at least 75% overall instructions, lines and methods, and 50% lines per class (existing exclusions apply).

### CI

`.github/workflows/main.yml` runs on every PR and push to `main`:

1. Frontend: `npm ci` → `npm run build` → `npm run test` → `npm run lint`
2. Backend: `./mvnw clean verify -Pcoverage`
3. Uploads both coverage reports as build artifacts.

Before opening a PR, run the same steps locally so the pipeline stays green.

## 5. Production mode

In production the frontend is bundled **inside** the backend jar and everything is served by Spring Boot from a
single origin (http://localhost:8080). The Maven `prod` profile:

1. Downloads Node/npm into `backend/target/`, runs `npm install` and `npm run build:prod` in `frontend/`.
2. Copies `frontend/dist/` into the jar's `static/` folder.
3. Activates the Spring `prod` profile (`pro_tube.load_initial_data=false`, `ddl-auto=update`).

`frontend/.env.production` leaves `VITE_API_DOMAIN`/`VITE_MEDIA_DOMAIN` empty, so the built SPA calls the API with
relative URLs (`/api`, `/media`) on the same host.

### Run directly with Maven

```bash
cd backend
mvn -Pprod spring-boot:run
```

(IntelliJ: the shared **Production** run configuration does the same.)

### Build and run the jar

```bash
cd backend
mvn -Pprod clean package
java -jar target/protube-back-0.0.1-SNAPSHOT.jar
```

(IntelliJ: the **Create jar** run configuration runs `package` with the `prod` profile.) Remember that
`ENV_PROTUBE_STORE_DIR`, `ENV_PROTUBE_DB_USER` and `ENV_PROTUBE_DB_PASS` must be set in the environment where the jar
runs.

To only preview the production frontend bundle, without the backend: `cd frontend && npm run build:prod && npm run preview`.

### Using PostgreSQL

Both profiles already use PostgreSQL. Start the database using the existing compose.yaml with ENV_PROTUBE_DB, ENV_PROTUBE_DB_USER and ENV_PROTUBE_DB_PASS set. Backend configuration uses ENV_PROTUBE_DB_HOST (default localhost) and the same database variables. No application.properties changes are required.

## 6. Workflow reminders

* `main` is protected: work in a branch and open a Pull Request; it needs at least one approval and all Copilot
  review comments resolved before merging.
* Use [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) and reference the task.
* If AI generated part of a change, mark it in the commit message and reference the prompt from `prompts/` that was
  used (see [prompts/README.md](./prompts/README.md)).
* Setting up the GitHub repository: [docs/GITHUB_SETUP.md](./docs/GITHUB_SETUP.md).

## Troubleshooting

* **`Could not resolve placeholder 'ENV_PROTUBE_STORE_DIR'`** (or `ENV_PROTUBE_DB_USER`, ...): the variable isn't
  set in the environment of the process running the app. IDEs usually need a restart to pick up new OS-level
  variables.
* **Videos list loads but thumbnails/videos 404**: check that `ENV_PROTUBE_STORE_DIR` ends with a path separator and
  points to the folder containing the `.mp4`/`.webp` files.
* **Frontend can't reach the API in dev**: make sure the backend is running on port 8080, or adjust
  `VITE_API_DOMAIN` in `.env.development.local`.
* **Port already in use**: change `server.port` (backend) or run `npm run dev -- --port 5174` (frontend).

## US-04 catalog import and manual checks

Both development and production use the existing PostgreSQL configuration. Set ENV_PROTUBE_DB_HOST (default localhost), ENV_PROTUBE_DB (default protube), ENV_PROTUBE_DB_USER and ENV_PROTUBE_DB_PASS to match your database.

1. Start PostgreSQL (the existing compose.yaml provides the database; set its environment variables first).
2. Point ENV_PROTUBE_STORE_DIR to the absolute existing videoGrabber store, including its trailing / or backslash. Each video needs matching <id>.json, <id>.webp and <id>.mp4 files. JSON must have a nonnegative integer id matching its filename and a nonblank title. Source ID zero is supported.
3. From backend/, run mvn spring-boot:run. The default dev profile imports metadata once at startup into PostgreSQL; images and video files stay in the configured directory. Invalid/incomplete entries are skipped with a warning. Missing directories do not prevent startup. Import never deletes existing rows or updates already imported IDs.
4. For a production initial import, explicitly set PRO_TUBE_LOAD_INITIAL_DATA=true for that startup; remove it afterward. Production disables initial import by default. Keep the same media store available after import.
5. From frontend/, run npm run dev (npm.cmd on Windows when PowerShell blocks npm.ps1) and visit http://localhost:5173/. Confirm titles and thumbnails without signing in. With more than 12 valid imported videos, use Next and Previous and verify different entries and page boundaries.
6. Register or log in, verify the catalog after returning home, then log out and verify browsing still works. Restart the backend and confirm imported entries remain without duplicates. Stop the backend to check the error/Retry state; an empty database with import disabled shows the empty state.

GET /api/videos?page=0&size=12 returns content summaries (id, title, thumbnailPath) and page, size, totalElements, totalPages. Pages are zero-based, ordered by database ID descending so newly uploaded videos appear first; size is bounded to 1–100. Relative thumbnail paths resolve under /media/ through the existing Env.ts media domain.

Validation: backend mvn clean verify (Java 21; Docker required for PostgreSQL Testcontainers); frontend npm.cmd run test -- --watchAll=false --ci, npm.cmd run lint, npm.cmd run build. Reports: backend/target/site/jacoco/index.html and frontend/coverage/index.html.

Source IDs identify an existing grabber dataset. Regenerating a different dataset with reused numeric IDs requires an explicit data-management decision; the importer deliberately does not overwrite records. Removing media files after import leaves their metadata in the catalog. Playback, uploads and search remain separate stories.

## US-03 profiles and manual checks

Profile fields are persisted on the existing PostgreSQL app_users table: displayName (up to 80 characters),
avatarUrl (up to 2048) and description (up to 1000). The existing ddl-auto=update configuration adds nullable
columns so accounts from Sprint 1 continue to work; unset fields display a default avatar and empty biography.
Avatar images use an HTTP/HTTPS URL. An empty URL restores the default avatar.

- GET /api/users/{id}/profile returns only id, displayName, avatarUrl and description to any visitor.
- GET /api/profile returns the authenticated user's profile.
- PUT /api/profile requires the session cookie and X-CSRF-TOKEN. It edits only the session owner's account,
  ignores submitted account IDs, validates field lengths and avatar URLs, and returns the saved public DTO.
- Browser routes: /profile for the current user; /users/{id} for a shareable public profile.

Manual validation:

1. Start the database/backend/frontend as above. Register or log in and open **My profile**.
2. Save a display name, an HTTP/HTTPS image URL and a description. Confirm the displayed profile updates,
   then reload and restart the backend to confirm persistence. Clear fields to restore the defaults.
3. Copy the **Public profile** link and open it in a private browser window or as a different registered user.
   Confirm public fields are visible, with no email, password information or edit controls.
4. Log out while viewing your own profile. Confirm the edit form disappears and sign-in is requested.
5. Open /users/999999999 to check not found. Stop the backend and use Retry to check recovery.
6. API checks: anonymous GET/PUT /api/profile is rejected, authenticated PUT without CSRF returns 403,
   invalid/oversized fields return 400 without changing the previous profile, and public GET stays available.

Automated validation: backend `mvn clean verify` (Java 21 and Docker); frontend
`npm.cmd run test -- --watchAll=false --ci --runInBand`, `npm.cmd run lint`, `npm.cmd run build`.
Prompt traceability: prompts/us-03-user-profile.prompt.md. Include it with the implementation in the feature PR.
