# Engineering decisions and review

## Architecture and ownership

This application uses a modular monolith. Feature packages keep authentication, organizations, projects, tasks, comments, labels, and activity separate. Transactions can span multiple tables without distributed coordination. There are no speculative service interfaces, message brokers, caches, or state-machine frameworks.

`OrganizationAccess` is the resource authorization boundary. It looks up an organization from the stored project/task relationship and checks an ACTIVE membership for the authenticated subject. `TaskPolicy` adds field-level rules for assigned members. `MembershipService.checkManage` prevents privilege escalation and owner removal. Frontend controls mirror these policies for usability but do not provide security.

All users are global identities. Roles live in `organization_members`. An OWNER in organization A has no authority in organization B unless they also hold an active membership there. All projects inherit their organization's membership. `ProjectMember` is deliberately absent until private project access has a concrete requirement.

## Persistence modeling

The database is normalized around users, memberships, projects, tasks, comments, and labels. Membership uses a UUID key plus a unique organization/user pair to simplify entity identity. `task_labels` has a composite primary key; its additional project ID and two composite foreign keys deliberately duplicate scope so PostgreSQL itself rejects cross-project associations.

Entities use explicit UUID references for aggregate relationships. This avoids accidental lazy-loading during serialization and lets services batch user/label lookups. The relational foreign keys still enforce references. A navigable `@ManyToOne` graph could also work, but would need careful fetch plans and DTO mapping. Neither approach removes the need to understand joins or query counts.

Hibernate uses field access. Mutable task/project/comment/organization entities have `@Version`. They are never sent directly to the browser. API records are mapped inside service transactions with Open Session in View disabled. Flyway creates the schema; Hibernate's `validate` checks compatibility on startup. Once shared, migrations should be followed by new versioned migrations rather than edited in place.

## Transactions, locks, and conflicts

Business writes acquire the organization row's pessimistic write lock before checking roles. All task assignment, membership removal, archive changes, and project writes follow this order. This makes membership changes and resource writes serialize and prevents an assignment from racing past a member's removal. The lock is held until commit or rollback.

Task creation reads/increments `next_task_number` while that lock is held. A unique project/number constraint provides an additional guard. We do not use `MAX(number) + 1`. A concurrent-creation integration test issues eight requests and asserts distinct identifiers.

Serialized writes alone do not protect a user's stale browser form. PATCH therefore requires the version they read. The service compares it after obtaining the lock, and Hibernate's version also protects persistence updates. A stale update receives 409. Labels and membership changes intentionally do not expose optimistic edit versions; those small operations serialize and use current state.

Data mutation and activity creation use the same transaction. Activity recording requires an existing transaction with `Propagation.MANDATORY`. A test modifies a title and attempts an invalid transition, then confirms both the title and audit count remain unchanged after rollback.

Trade-off: locking at organization scope is coarse. Independent writes in one busy organization serialize. Before optimizing, narrow the lock protocol while retaining assignment/removal, owner, archive, and task-number invariants in concurrency tests. Do not simply remove the lock after adding optimistic versions: they solve different problems.

## Authentication details

Passwords use BCrypt with a production work factor of 12. Tests use factor 4 through a test-only primary bean so authentication coverage stays practical. Registration rejects inputs beyond BCrypt's 72-byte UTF-8 limit. Unknown-user login still runs a BCrypt comparison against a dummy hash; login errors do not distinguish missing users from bad passwords. Registration currently reports duplicate email, which can reveal account existence; that is an explicit limitation.

JWTs use HS256, a configured secret of at least 32 bytes, a fixed issuer and audience, an issued-at time, expiration, and token ID. Spring Security verifies the signature and configured claims. Tokens do not carry organization roles, so active role changes apply to subsequent service calls. Tests check expired tokens, wrong issuer, wrong audience, tampered signatures, and missing credentials.

Refresh tokens are opaque 256-bit random values. SHA-256 is appropriate for hashing these uniformly random credentials; it would not be appropriate as a password hash. A token record retains its user, family, expiry, used time, and revoked time. Replacement tokens retain the family's absolute expiration. Consumed token rows remain available for replay detection until the family expires.

Refresh and logout lock the user row, then read current token state. This serializes family revocation with token issuance. When reuse is detected, the service revokes the family and returns an empty result. The controller raises 401 after the service transaction commits; throwing a rollback-triggering exception inside that transaction would otherwise undo revocation.

The frontend coordinates refresh inside a tab and, where supported, across tabs with Web Locks. It retries an authenticated resource request at most once. Access tokens stay in memory. Refresh cookies are HttpOnly and SameSite=Strict. CSRF uses a separate readable cookie and request header. Login/register are also CSRF-protected. Deploy frontend and API on the same site, preferably the same origin.

Logout revokes refresh capability, not already-issued access JWTs. Immediate logout revocation would require checking a session/revocation store or changing the token strategy. This version makes the 15-minute upper bound explicit.

## Security review

| Area | Implementation / evidence | Remaining consideration |
|---|---|---|
| Password storage | BCrypt; hash verified in API test; byte-limit unit test | Calibrate work factor on target host; add recovery and verification |
| JWTs | Framework verification; expiry/issuer/audience/signature negative tests | Signing-key rotation and multi-instance key management |
| Refresh credentials | Random tokens, hashes, rotation, replay family revocation | Periodic cleanup after absolute expiry; session-management UI |
| Tenant isolation | Service resource lookups and membership checks; outsider tests across APIs | Review every new endpoint against the same policy |
| Privilege escalation | Owner/admin boundaries and field-level task rules tested | Ownership-transfer workflow intentionally absent |
| IDOR | Task/comment/label/project scope checked from persisted data | UUID randomness is not relied upon for access control |
| SQL injection | Bound JPA predicates; strict sort allowlist; literal search wildcard escaping | Keep future raw SQL parameterized |
| Mass assignment | DTOs and PATCH field allowlists | Update allowlists deliberately when extending requests |
| CSRF / CORS | CSRF header-cookie pair for auth POSTs; explicit allowed origin | Use HTTPS and secure cookies outside local development |
| Errors | Safe centralized JSON errors; no stack traces in responses | Server logs need appropriate access and retention |
| XSS | React text rendering, no arbitrary HTML injection | A compromised page can still act as the user; add deployment CSP |
| Abuse | Bounded task/comment/activity pages and request-field sizes | Add login/registration throttling and overall body limits before public exposure |
| Deletion | FK cascades and service permissions tested | No backup/restore automation or compliance retention |

The application is not presented as production hardened. No penetration test has been performed. Email verification, password reset, MFA, authentication throttling, security headers at the final TLS ingress, backup restoration, and operational monitoring need decisions before exposing it publicly.

## Query and performance review

Task listing performs an authorized project lookup, a paginated task query (with a count when Spring Data needs one), a batched user lookup, and a batched label projection. It does not join-fetch a many-to-many collection while applying pagination. Comments and activity also batch author lookups. Project and organization lists avoid a separate membership lookup per displayed item.

The integration suite measures Hibernate prepared-statement counts for one task and thirteen tasks in a result page and asserts that the query count does not grow. This is an N+1 regression check, not a throughput or latency benchmark. Statistics use `GROUP BY status` in the database.

The migration contains indexes corresponding to the implemented access paths: organization membership, project task listing, status, assignment, task comments, labels, and entity/project/organization history. Primary/unique constraints already provide their indexes. A plain B-tree does not optimize arbitrary leading-wildcard substring search; evaluate `pg_trgm` or full-text search with representative data.

HikariCP has a maximum pool size of 10. This is configuration, not evidence that 10 is optimal. Pool sizing depends on query duration, database capacity, and workload. OFFSET pagination is simple and sufficient for this project; deep pages and constantly changing data can benefit from cursor pagination later.

Administrative lists are currently unpaginated. Membership removal fetches assigned tasks in batches and records changes in one transaction; the persistence context and transaction still grow with total work. Very large organizations need a revised strategy. Organization locking, substring search, large deletions, and list sizes are documented limitations, not measured scalability claims.

## Audit behavior

Activity records capture an actor, action, entity, scope, timestamp, and focused old/new values. They are append-only through the API, but privileged database users can modify them. Deleting an organization removes its history. This is business history, not a tamper-evident legal archive.

Application logs describe runtime failures. Activity logs describe business changes. Event sourcing would rebuild current business state from an authoritative event stream. This application stores current state in ordinary tables and supplements it with history.

## Review findings addressed

- Database-level project consistency added to label associations.
- Refresh replay revocation commits before a 401 is returned.
- Membership removal and assignment follow the same lock protocol.
- Stale-edit versions are checked after acquiring the lock.
- All task fields are authorized as a set, avoiding a partial unauthorized PATCH.
- Task user/label data is fetched in batches; query growth is regression-tested.
- Description/comment bodies are excluded from audit snapshots.
- Explicit null and omitted PATCH fields remain distinguishable.
- Frontend refresh is bounded and deduplicated; error responses remain visible.
- Windows npm scripts invoke JavaScript entry points directly to support workspace paths containing `&`.
- The Vitest dependency was updated after a package audit reported a vulnerable transitive version.
- Form labels use explicit label/control associations so selects have clear accessible names.
- A browser regression exposed CSRF invalidation after bearer authentication. The client now caches only an in-flight bootstrap and retries rejected authentication CSRF checks at most once; reload/logout is browser-tested.
- The mobile layout retains a visible logout control.
- The Windows launcher runs a copy of the packaged jar, allowing Maven to rebuild without conflicting with the running JVM's file handle.

## Sources

The implementation uses the [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Spring Security JWT reference](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html), [password-storage reference](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html), [CSRF reference](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html), [PostgreSQL constraints documentation](https://www.postgresql.org/docs/18/ddl-constraints.html), and [Springdoc documentation](https://springdoc.org/). Version pins and tests in this repository are the reproducible record of the selected stack.
