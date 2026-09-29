# Development phase ledger

The original plan requested a stop after each phase. The subsequent instruction authorized completing the project in one continuous implementation pass. This ledger preserves the requested order and links each phase to concrete source and validation. All code is stored as complete files in the repository.

| Phase | Delivered artifacts | Check / expected behavior |
|---|---|---|
| 1: requirements/design | README, engineering guide, API contract | Organization scope, roles, relationships, workflow defined |
| 2: backend initialization | `backend/pom.xml`, wrapper, application class, configuration | Maven compiles Java 21 sources |
| 3: user/PostgreSQL/migration | `user/`, `db/migration/V1__initial_schema.sql` | Flyway creates schema; Hibernate validates |
| 4: authentication | `auth/` | Register/login/refresh/logout/me; BCrypt and JWT negative tests |
| 5: organizations/RBAC | `organization/` | Creator is owner; escalation/removal restrictions enforced |
| 6: projects | `project/` | Admin CRUD, immutable keys, archive/restore |
| 7: tasks | `task/TaskService`, DTOs/controller/repository | Create/read/update/delete; unique project numbers |
| 8: workflow/assignment | `TaskWorkflow`, `TaskPolicy`, membership removal | Every transition pair tested; assignment scoped to active members |
| 9: comments/labels | `comment/`, `label/` | Author edits, moderation, same-project label constraints |
| 10: activity | `activity/` | Activity and mutations share transactions; pagination |
| 11: queries | `TaskSpecifications`, `Pages` | SQL filters/search/sorting/pagination; bounded page sizes |
| 12: backend tests | `backend/src/test/` | `mvnw verify` runs units and PostgreSQL API tests |
| 13: OpenAPI | Springdoc dependency and security scheme | `/v3/api-docs` and `/swagger-ui` |
| 14: React initialization | Vite config, package lock, main/App | `npm ci`, `npm run build` |
| 15: frontend auth | AuthContext, client, AuthPage | In-memory access token, cookie refresh, protected routing |
| 16: workspace UI | DashboardPage, OrganizationPage, layout | Create organizations/projects; switch organizations; manage members |
| 17: task UI/statistics | ProjectPage, TaskForm/Card, TaskPage | Board/list, filters, properties, SQL-derived status totals |
| 18: collaboration UI | Comments, Activity, member/label management | Add/edit/delete comments, read history, manage permitted roles |
| 19: frontend tests | RTL/Vitest tests and Playwright workflow | Auth/task/permission/client tests; full browser flow |
| 20: containers | Dockerfiles, nginx config, Compose, env scripts | Defined; runtime verification requires Docker |
| 21: CI | `.github/workflows/ci.yml` | Backend/frontend/Compose browser jobs; no deployment |
| 22: security review | `docs/engineering.md`, negative tests | Isolation, JWT, CSRF, mass assignment, permission boundaries |
| 23: database/performance review | Query-count regression test, index review | Task-list queries do not grow per task; limitations documented |
| 24: documentation | README and `docs/` | Setup, exact commands, contracts, explanations, limitations |
| 25: final review | Form associations, error handling, dependency and formatting fixes | Rerun affected checks; final outcomes in verification record |
| 26: interview preparation | `docs/interview.md` | Questions ready; interactive answers require the learner |

## Commands and expected outcomes

From `backend/`, `mvnw.cmd test` (or `bash mvnw test`) runs unit tests and should finish with `BUILD SUCCESS`. `mvnw.cmd verify` adds API integration tests using PostgreSQL from Testcontainers, or an explicit disposable `TEST_DB_URL`. No Docker-unavailable skip hides missing integration coverage.

From `frontend/`, `npm ci` installs the lockfile, `npm test` reports passing test files, and `npm run build` writes `dist/`. With the application running, `npm run test:e2e` runs the browser workflow. See the README for browser installation and base URL settings.

From the root, generate `.env` using the supplied script. `docker compose up --build -d --wait` should build services and wait for healthy startup. This command must still be executed on a Docker-capable machine; local Java/Node/PostgreSQL checks do not prove image builds.

## Manual acceptance checklist

- Register two accounts. Create an organization with the first, add the second as MEMBER, and log in as each in separate browser profiles.
- Confirm the member sees projects but cannot create/delete them or promote anyone, including through direct HTTP requests.
- Create a project/task as owner, assign it to the member, and transition it through the permitted states.
- Attempt TODO → DONE directly; expect 409 and unchanged state/history.
- Open the same task in two tabs, save one, then save the other with its stale version; expect 409.
- Create a label in another project and try attaching it; expect rejection.
- Add comments as both users. Edit your own, try editing the other's, then test owner moderation deletion.
- Remove the member. Their next protected resource request should fail, and their task assignments should clear.
- Archive a project. Reads continue; task/comment/label mutations fail until restoration.
- Refresh the page while signed in, then log out and revisit a protected route.
- Apply filters and navigate pages; confirm the UI does not imply the current board page is the whole project.
- Review actual test reports and known limitations before describing the project in an interview or resume.
