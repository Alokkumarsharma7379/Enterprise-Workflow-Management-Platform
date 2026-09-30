# Verification record

Last verified locally: **2026-09-30**. This record distinguishes executed tests from infrastructure that has only been configured.

## Executed checks

| Check | Result | Evidence |
|---|---|---|
| Maven Java compilation and packaging | Passed | `mvnw.cmd -B verify` completed with BUILD SUCCESS |
| Backend unit tests | 46 passed, 0 failed, 0 skipped | `backend/target/surefire-reports/TEST-*.xml` |
| PostgreSQL API/integration tests | 20 passed, 0 failed, 0 skipped | `backend/target/failsafe-reports/TEST-com.example.workflow.WorkflowApiIT.xml` |
| Clean frontend lockfile install | Passed | `npm ci` using Node 22 and npm 10 |
| Frontend component/client tests | 12 passed across 4 files | `npm test` |
| Frontend production build | Passed | `npm run build`, output under `frontend/dist/` |
| Frontend dependency audit | No reported vulnerabilities | `npm audit --audit-level=moderate` and clean-install audit |
| Browser workflow | 1 passed | Playwright with installed Chrome against the running React/Spring/PostgreSQL application |
| Screenshots | Captured and visually inspected | `docs/screenshots/project-board.png`, `task-details.png` |
| Git whitespace check | Passed | `git diff --check` |
| Windows convenience launcher | Passed | `scripts/start-local.ps1 -SkipBuild` started the packaged backend and Vite with locally generated secrets |
| Health and interactive API schema | Passed | `/actuator/health` returned UP; `/v3/api-docs` exposed 23 paths with bearer security and the authentication CSRF header |

Local runtimes used: Java 21.0.5 from the existing JetBrains installation, PostgreSQL 18.1 from the installed PostgreSQL distribution, Node 22.23.3 from an isolated local tool directory, Maven 3.9.11 via the wrapper, and Vitest 4.1.11. These are the observed development runtimes, not a recommendation to retain old runtime patch versions for a public deployment.

The PostgreSQL tests used a dedicated `workflow_test` database in an isolated workspace cluster bound to 127.0.0.1:55432. They exercised actual migrations, constraints, JPA queries, security filters, controllers, and service transactions. They did not substitute H2 or silently skip integration coverage.

## Behaviors covered

- Registration, normalized emails, BCrypt storage, login, authenticated access, invalid credentials, and validation errors.
- JWT signature, expiration, issuer, and audience validation.
- Cookie CSRF protection, refresh rotation, replay family revocation, and logout revocation.
- MEMBER organization deletion/promotion denial, ADMIN project creation, MANAGER task management, protected owner membership.
- Outsider access denial across organizations, members, projects, tasks, labels, comments, statistics, and activity.
- Comment author editing, another member's edit denial, admin edit denial, and admin deletion moderation.
- Assigned-member status editing, prohibited field updates, invalid transitions, stale versions, and terminal states.
- Atomic rollback of task changes and activity records.
- Membership removal unassigning tasks and blocking subsequent access.
- Search, filters, label projection, pagination bounds, sort allowlist, priority order, and literal search wildcard handling.
- Service and database rejection of cross-project labels; rejection of outside assignees.
- Explicit-null PATCH versus omitted values and mass-assignment rejection.
- Archive/restore rules, concurrent task-number allocation, and deletion cascades/history retention.
- Task-page prepared-statement count remaining constant as returned task count grows.
- Frontend login submission/error display, task rendering/creation/errors, permission-sensitive controls, deduplicated refresh, bounded retries, and fresh CSRF bootstrap.
- Browser registration → organization → project → assigned task → status change → comment → activity → page reload → logout at mobile width → protected-route redirect.

The browser test uses the production password encoder and real HTTP/cookies. The backend suite uses a test-only lower BCrypt cost. The browser flow exposed issues not caught by mocked component tests: implicit select label naming and cached CSRF state after authentication. Both were fixed before the recorded passing run.

## Not executed here

- Docker image builds or `docker compose up`: Docker was not installed/available.
- Testcontainers startup: integration tests used the documented external PostgreSQL option instead.
- GitHub-hosted CI: workflow files are present but no remote run was triggered.
- Public deployment, penetration testing, load testing, capacity testing, backup/restore drills, or long-running reliability tests.

The CI definition includes Docker/Testcontainers and a Compose browser job so those paths can be verified on a Docker-capable runner. Their existence must not be described as an observed successful CI run. There are no fabricated user counts, latency measurements, uptime figures, or scalability claims.

## Reproducing the suite

Follow the README's testing commands. Testcontainers is the default when `TEST_DB_URL` is absent. When supplying that variable, use a disposable database whose name ends in `_test`; the suite truncates application tables before each API test.

The final Docker acceptance step is:

```sh
docker compose up --build -d --wait
```

Then run the browser suite with `E2E_BASE_URL=http://localhost:8081`. Review the resulting reports before marking container verification complete or making deployment/CI claims.
