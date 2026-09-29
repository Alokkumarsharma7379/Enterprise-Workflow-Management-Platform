# API contract

All paths below are relative to `/api`. JSON is the request/response format. IDs are UUID strings. Authentication requires `Authorization: Bearer <accessToken>` except the public authentication endpoints. The bearer token never grants organization authority by itself: services check current memberships.

## Authentication

| Method and path | Request | Result |
|---|---|---|
| GET `/auth/csrf` | No body | CSRF cookie and `{token}` |
| POST `/auth/register` | `email`, `password`, `displayName` | 201, access token + user, refresh cookie |
| POST `/auth/login` | `email`, `password` | 200, access token + user, refresh cookie |
| POST `/auth/refresh` | Refresh cookie; no body | 200, replacement tokens |
| POST `/auth/logout` | Refresh cookie; no body | 204, revoke family and clear cookie |
| GET `/auth/me` | Bearer token | `id`, `email`, `displayName` |

All authentication POSTs require `X-XSRF-TOKEN` matching the CSRF cookie issued by the bootstrap request. Keep the same cookie jar. Passwords require at least 12 characters on registration and cannot exceed 72 UTF-8 bytes. Returned tokens and password fields must not be logged.

PowerShell example against a native backend:

```powershell
$base = 'http://localhost:8080/api'
$csrf = Invoke-RestMethod "$base/auth/csrf" -SessionVariable workflowSession
$csrfHeaders = @{ 'X-XSRF-TOKEN' = $csrf.token }
$password = Read-Host 'Choose a password (12+ characters)' -AsSecureString
$credentials = @{
    email = 'your-account@example.com'
    displayName = 'Your name'
    password = [Net.NetworkCredential]::new('', $password).Password
} | ConvertTo-Json
$session = Invoke-RestMethod "$base/auth/register" -Method Post `
    -WebSession $workflowSession -Headers $csrfHeaders `
    -ContentType 'application/json' -Body $credentials
$authorization = @{ Authorization = "Bearer $($session.accessToken)" }
$organization = Invoke-RestMethod "$base/organizations" -Method Post `
    -Headers $authorization -ContentType 'application/json' `
    -Body (@{name='Engineering'} | ConvertTo-Json)
```

The example masks password entry and keeps tokens in variables rather than printing them. For an existing account use `/auth/login` with email/password only. The request necessarily contains the password in process memory; use HTTPS outside local development.

## Organizations and members

| Method and path | Body / behavior |
|---|---|
| GET `/organizations` | Active organizations with caller's role |
| POST `/organizations` | `{name}`; creator becomes owner |
| GET `/organizations/{id}` | Organization and caller's role |
| PATCH `/organizations/{id}` | `{version, name?}`; owner only |
| DELETE `/organizations/{id}` | Owner only; cascades contained data |
| GET `/organizations/{id}/members` | Active member user IDs, display names, roles |
| POST `/organizations/{id}/members` | `{email, role}`; existing registered user |
| PATCH `/organizations/{id}/members/{userId}` | `{role}` |
| DELETE `/organizations/{id}/members/{userId}` | Marks membership REMOVED and unassigns tasks |
| GET `/organizations/{id}/activity` | Paginated organization history |

Emails are not included in member list responses. Administrators must know the email of a registered user to add them. Re-adding a removed user reactivates the existing membership; old tasks remain unassigned.

## Projects

| Method and path | Body / behavior |
|---|---|
| GET `/organizations/{id}/projects` | Projects in organization |
| POST `/organizations/{id}/projects` | `{name, key, description?}` |
| GET `/projects/{id}` | Project details, current caller role, version |
| PATCH `/projects/{id}` | `{version, name?, description?, status?}` |
| DELETE `/projects/{id}` | Delete contained tasks, comments, labels |
| GET `/projects/{id}/statistics` | Total and counts keyed by all five task statuses |
| GET `/projects/{id}/activity` | Paginated project history |

Project keys are 2–10 ASCII letters/digits, starting with a letter, uppercased and unique per organization. Keys cannot change. Status is ACTIVE or ARCHIVED. Only owners/admins manage projects.

## Tasks

| Method and path | Body / behavior |
|---|---|
| POST `/projects/{id}/tasks` | `{title, description?, priority, assigneeId?, dueDate?}` |
| GET `/projects/{id}/tasks` | Paginated filtered tasks |
| GET `/tasks/{id}` | Task with reporter/assignee summaries and labels |
| PATCH `/tasks/{id}` | `{version, title?, description?, status?, priority?, assigneeId?, dueDate?}` |
| DELETE `/tasks/{id}` | Delete task, comments, and label links |
| GET `/tasks/{id}/activity` | Paginated task history |

Tasks start in TODO. Priority is LOW, MEDIUM, HIGH, or CRITICAL. Due dates use `YYYY-MM-DD`; timestamps use ISO-8601 instants. Members may edit only title, description, and status on their own assigned tasks. Managers/admins/owners can edit all supported fields.

PATCH example:

```json
{
  "version": 3,
  "status": "IN_REVIEW",
  "assigneeId": null
}
```

This transitions and unassigns atomically if authorized. An omitted `dueDate` stays unchanged. Explicit `dueDate: null` clears it. A forbidden field causes the entire update to fail. `reporterId`, `projectId`, IDs, timestamps, and other server-controlled fields cannot be assigned through this request.

Query parameters:

| Parameter | Values |
|---|---|
| `search` | Literal case-insensitive substring in title/description, at most 200 characters |
| `status` | One workflow status |
| `priority` | One priority |
| `assigneeId` | User UUID |
| `labelId` | Label UUID |
| `page` | Zero-based, default 0 |
| `size` | 1–100, default 20 |
| `sort` | `createdAt`, `updatedAt`, `priority`, or `dueDate`, followed by `,asc` or `,desc` |

```text
/projects/{id}/tasks?status=IN_PROGRESS&priority=HIGH&page=0&size=20&sort=dueDate,asc
```

Priority sorts by its business ranking, and due-date sorting keeps undated tasks last. Every sort adds an ID tie-breaker. Task filtering and pagination occur in SQL, not in-memory collections. The response envelope is:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## Comments and labels

| Method and path | Body / behavior |
|---|---|
| GET `/tasks/{id}/comments` | `page`, `size`; newest first |
| POST `/tasks/{id}/comments` | `{body}`; active members |
| PATCH `/comments/{id}` | `{version, body}`; author only |
| DELETE `/comments/{id}` | Author, owner, or admin |
| GET `/projects/{id}/labels` | Project labels |
| POST `/projects/{id}/labels` | `{name, color}`; managing roles |
| PATCH `/labels/{id}` | `{name?, color?}` |
| DELETE `/labels/{id}` | Delete label and its task associations |
| PUT `/tasks/{taskId}/labels/{labelId}` | Idempotently attach a label from the same project |
| DELETE `/tasks/{taskId}/labels/{labelId}` | Idempotently detach the label |

Comments are plain text, maximum 10,000 characters. Label names are lowercase and unique per project; colors use six-digit hexadecimal notation, such as `#557b69`. Task descriptions and project descriptions have a 10,000-character request limit.

## Activity and statistics

Activity endpoints accept `page`/`size`, return newest first with an ID tie-breaker, and expose actor summaries, action type, entity type/ID, old/new values, and timestamps. Description/comment bodies are not copied into activity records. Comment events store the comment ID. Assignment events store user IDs so the log remains structured.

Statistics aggregate all project tasks in SQL and include TODO, IN_PROGRESS, IN_REVIEW, DONE, and CANCELLED. They are not limited to a task-list page or its filters.

## Errors

Errors return `timestamp`, `status`, `error`, `message`, `path`, and a `fields` object for field-validation errors. They do not include Java stack traces. Codes: 400 invalid request, 401 missing/invalid authentication, 403 forbidden or invalid CSRF, 404 missing resource, 405 unsupported method, 409 conflict/stale version/invalid transition, and 500 unexpected failure.

An authenticated outsider receives 403 for an existing inaccessible organization resource. This deliberately distinguishes access failure from a nonexistent ID; no protected content is returned. UUIDs alone are not authorization.

## Deletion and concurrency

Owner deletion of an organization deletes its activity too. Task deletion retains task activity at project scope, and project deletion retains activity at organization scope with a null project reference. These logs are a collaboration history, not tamper-proof compliance storage.

Writes acquire an organization lock before checking roles and modifying related resources. Edits additionally compare the client's version. Task number allocation is transactional and serialized. A failed operation rolls back both data and activity. Administrative lists and membership removal are intended for modest project sizes; see engineering limitations before scaling.
