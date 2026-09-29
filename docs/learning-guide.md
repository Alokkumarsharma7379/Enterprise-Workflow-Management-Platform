# Learning the implementation

Read this alongside the source, then explain each decision in your own words. The aim is to trace real behavior, not memorize definitions.

## Begin with one request

Follow a PATCH to `/api/tasks/{id}`:

1. Spring Security's bearer-token filter validates the JWT and installs an authenticated principal.
2. Spring MVC selects `TaskController.update`, parses the UUID, and reads JSON.
3. `TaskService.update` begins a transaction through Spring's proxy.
4. It loads the task and resolves its project/organization. `OrganizationAccess` locks the organization and checks the caller's active membership.
5. The service refreshes task state, rejects an archived project, checks editable fields, and compares the client's version.
6. `Patch` validates each supplied value, and `TaskWorkflow` validates status transitions.
7. Hibernate tracks entity changes. `ActivityService.record` adds focused history within the same transaction.
8. Flushing sends changes to PostgreSQL. The version detects conflicting persistence updates; constraints protect relational integrity.
9. The service maps data into `TaskDtos.View`. The transaction commits, and Jackson serializes the DTO.
10. If a business exception escapes the service, changes roll back. `ApiErrors` converts it into an HTTP error without a Java stack trace.

Exercise: place breakpoints at these boundaries and compare a successful TODO → IN_PROGRESS request with a rejected TODO → DONE request.

## Java concepts in this repository

| Concept | Example and reason |
|---|---|
| Class | `TaskService` groups related operations and injected dependencies |
| Interface | `TaskRepository` defines persistence capabilities implemented by Spring Data |
| Record | `TaskDtos.View` is a concise immutable API data carrier |
| Enum | `TaskStatus` restricts possible states and makes comparisons explicit |
| Generics | `JpaRepository<Task, UUID>` expresses the entity and identifier types; `Pages.Result<T>` reuses an envelope safely |
| List | Ordered task responses and comments |
| Set | Allowed transitions/fields and deduplicated user IDs |
| Map | User ID → user and status → count lookups |
| Exceptions | `ApiException` distinguishes expected business failures from unexpected errors |
| Streams | Transforming repository results into DTOs and grouping label projections |
| Optional | `findById` explicitly represents a possibly missing resource |

Records are shallowly immutable: a record holding a mutable list does not automatically make that list immutable. `Optional` is useful at lookup boundaries; it is not a substitute for every nullable database field. Streams simplify transformations, but should not hide expensive repository calls inside per-row mapping.

## Spring and dependency injection

The IoC container constructs and wires application objects. Constructor injection makes dependencies visible and permits unit tests to instantiate services with Mockito mocks. Spring controls the lifecycle rather than every service creating its own collaborators with `new`.

- `@Component`: a generic managed component, such as `Actor` or `OrganizationAccess`.
- `@Service`: a component with business/application behavior.
- Spring Data repository interfaces: Spring creates implementations and persistence exception translation infrastructure. A handwritten repository could use `@Repository`.
- `@RestController`: combines controller discovery and response-body serialization.
- `@Configuration` and `@Bean`: explicitly construct infrastructure, such as encoders and the security filter chain.
- `@Transactional`: advice wraps a public service invocation with transaction behavior.

Spring commonly implements these cross-cutting behaviors using proxies. Calling a transactional method directly from another method on the same object can bypass its proxy. Activity recording is a separate injected service and requires an existing transaction; it does not silently create an unrelated audit transaction.

The security filter chain runs before the controller. Security failures may never reach `@RestControllerAdvice`, which is why matching authentication-entry-point and access-denied handlers also produce JSON errors.

## Layers and responsibilities

An entity represents persisted state. A DTO represents an input or output contract. A repository handles persistence access. A service applies business rules, authorization, and transaction boundaries. A controller adapts HTTP to service calls.

Consider assignment: `@NotNull` can validate a required value, but it cannot determine whether a referenced user is an active organization member. That lookup-dependent rule belongs in a service/policy. Conversely, a 201 response or a request route belongs in the controller rather than the task entity.

Manual mapping is intentional. It makes privacy choices explicit: member lists include display names, not password hashes or email addresses. Returning an entity directly risks exposing fields added in the future and serializing unwanted persistence relationships.

## Relational modeling

A primary key identifies one row. A foreign key requires a referenced row. A unique constraint enforces a business uniqueness rule, such as one project key per organization. Null means absence; it is not an empty string or zero.

Normalization avoids storing independently changing facts repeatedly. User identity belongs in `users`; membership roles belong in `organization_members`. A user can be an owner in one organization and a member in another without duplicating their password or profile.

One organization has many projects: a one-to-many relationship. Many tasks can carry many labels: a many-to-many relationship represented by `task_labels`. A join queries related rows; a join table stores associations. They are related ideas, not interchangeable terms.

The repeated `project_id` in `task_labels` is deliberate controlled redundancy. Composite foreign keys use it to enforce the same-project rule at database level. A service check improves the error message; the constraint remains the final integrity guard.

An index provides a lookup structure. A composite index is ordered by multiple columns; leading columns affect which queries it supports. Indexes consume storage and make writes more expensive. A unique constraint already has a backing index in PostgreSQL, so adding an identical index wastes work.

Exercise: inspect `EXPLAIN (ANALYZE, BUFFERS)` for a project/status task query on your own sample data. A sequential scan over a tiny table is not automatically a defect. Explain plans describe a particular query and dataset; they are not evidence of production throughput.

## Transactions and ACID

Atomicity means a business operation commits as a whole or rolls back. Consistency means committed data satisfies enforced rules. Isolation describes what concurrent transactions can observe and how they interact. Durability means committed changes survive according to the database's configured guarantees.

Changing a task and appending its history must be atomic. Otherwise a failure could leave an updated task without its history, or a history event describing an update that never happened.

PostgreSQL's default READ COMMITTED isolation does not make `MAX(task_number) + 1` safe: two transactions can observe the same maximum. This project serializes allocation with a lock and still uses a unique constraint. Optimistic versions solve a separate problem: detecting when a client edited stale data.

Exercise: run the concurrent task creation test and the stale-version test. Explain why one should allow both requests with distinct numbers while the other should reject one conflicting edit.

## Authentication and authorization

A password proves credentials at login. BCrypt hashes it with salt and computational cost. We cannot decrypt the hash; verification compares a candidate through the encoder.

An access JWT is a signed claim about identity and expiry. Its contents are encoded, not encrypted, so secrets do not belong inside it. The server verifies the signature, issuer, audience, and expiry. Its signature alone does not decide whether a user may delete a particular organization.

A refresh token obtains another short-lived access token. Longer-lived credentials need revocation and replay handling, which is why refresh tokens have database records. Stateless bearer authentication does not imply that the entire application has no server-side state.

401 means authentication is missing or invalid. 403 means access is forbidden; CSRF failures also use 403. A member calling organization deletion with a valid JWT receives 403. An expired JWT receives 401 before the service runs.

An IDOR occurs when changing a resource ID lets a user access another person's protected data. Random UUIDs reduce guessing but do not provide authorization. Always resolve the resource's actual organization and check membership.

CSRF matters when browsers automatically attach credentials such as cookies. An Authorization header explicitly supplied by the application has a different threat model. This project protects cookie-based authentication operations with a matching CSRF token and restricts cross-origin requests. XSS remains a separate concern: avoid raw HTML rendering and protect the served application.

## JPA, Hibernate, and performance

JPA is the persistence specification. Hibernate implements it. Spring Data JPA supplies repository infrastructure on top. A managed entity's changes can be detected and written during flush, so not every update needs another `save` call.

Lazy loading defers related data until accessed. Eager loading requests it up front; neither automatically gives the right query plan. An N+1 pattern occurs when a list query is followed by another query for every item. This project uses UUID references, bulk user reads, and label projections to keep task-page queries bounded.

A DTO projection can select only needed columns rather than constructing full entities. Pagination limits rows in the database using limit/offset. Connection pooling reuses database connections; it does not make slow queries fast. Pool size, indexes, batching, and caching should follow workload evidence.

Exercise: read `listingQueryCountDoesNotGrowWithTaskCount`. Explain why it checks query growth rather than asserting an arbitrary latency threshold.

## React behavior

The router chooses pages. `AuthContext` holds the current user; the API client holds the access token in memory. On reload, the client uses the refresh cookie to restore authentication. A centralized API client prevents inconsistent token/header/error handling.

`useResource` ignores stale responses using a sequence counter, so a slower request cannot overwrite the result of a newer request. The server remains the source of truth after edits. Buttons are disabled while a mutation is pending, and server errors stay visible.

Forms use explicit label/control associations. The board shows one paginated result set and tells the user that columns reflect that page. Tests assert behavior such as submitted fields, visible errors, and hidden permission-sensitive controls rather than duplicating every implementation detail.

## Infrastructure

Docker images package runtimes and application artifacts. A multi-stage build keeps compilers out of the runtime image. Compose connects the API, web server, and database, waits for health checks, and persists the database volume. Environment variables provide configuration; they are not inherently secret storage, and must not be printed or bundled into browser code.

GitHub Actions repeats build and test steps on push/PR. A workflow file is a configured pipeline; only an executed run proves that it passes on a hosted runner. This project does not automatically deploy.

## Suggested study order

Trace task creation, trace status changes, explain the schema, explain membership permissions, trace refresh-token rotation and replay, run negative authorization tests, inspect query counts, then describe Docker and CI. Use the interview prompts after you can navigate each path in the code without guessing.
