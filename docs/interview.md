# Interview practice

Answer aloud before looking at source or the learning guide. Use a concrete request or test from this project in every answer. Do not claim features that are only listed as future work.

## Start here

**A MEMBER sends `DELETE /api/organizations/{id}` with a valid JWT. Trace the request and explain why it receives 403. Where is the organization-specific role checked, and why would hiding the frontend delete button be insufficient?**

Use that explanation as the first interactive interview answer. A reviewer can then identify gaps before proceeding.

## Java and Spring

1. Why did this project use Spring Boot and a modular monolith?
2. What happens between a request reaching the server and a controller returning JSON?
3. What is dependency injection? Show a constructor and a Mockito test that uses it.
4. What does Spring create for `TaskRepository`?
5. Where are records, enums, generics, collections, Optional, and exceptions useful here?
6. How is `@Service` different in intent from `@RestController`?
7. How does Spring apply `@Transactional`, and what is the self-invocation limitation?
8. What would happen if activity recording opened an independent transaction?

## Security and tenancy

9. Explain authentication versus authorization using a real endpoint.
10. What claims are in the access token, and what is deliberately omitted?
11. Why keep organization roles out of the JWT?
12. Why hash passwords with BCrypt but random refresh tokens with SHA-256?
13. What happens after refresh-token replay? Why must revocation commit before returning an error?
14. What does logout revoke, and what remains valid temporarily?
15. Where are access and refresh tokens stored in the browser? What are the trade-offs?
16. Which endpoints require CSRF protection and why?
17. What does CORS protect, and what does it not replace?
18. Demonstrate the difference between 401, 403, and 404.
19. What is IDOR? Show a test that changes IDs across organizations.
20. Can an admin promote themselves, rewrite another person's comment, or remove the owner?

## Data and transactions

21. Why PostgreSQL instead of a document database for this model?
22. Why is membership a separate table rather than a role column on users?
23. Explain the label join table and its composite foreign keys.
24. Why use Flyway and Hibernate validation instead of `ddl-auto=create`?
25. How do concurrent task creations get distinct project numbers?
26. What race exists between membership removal and assignment?
27. Why use both locking and edit versions?
28. Explain a transaction rollback using the title/status integration test.
29. How are deletes propagated, and which activity remains afterward?
30. What distinguishes business history, application logs, and event sourcing?

## APIs, frontend, and performance

31. Why return DTOs instead of JPA entities?
32. How does PATCH distinguish omitted fields from explicit null?
33. Why reject an entire PATCH if one field is forbidden?
34. How are priority sorting, wildcard search, and stable pagination implemented?
35. What is N+1, and what does the query-count test actually prove?
36. Explain lazy/eager loading even though this implementation uses explicit aggregate IDs.
37. Which indexes support current queries? Which query would a normal B-tree not optimize?
38. Why does the board describe its current page instead of implying it shows every task?
39. How does the API client avoid simultaneous refresh requests and infinite retries?
40. How does the UI avoid a slow response overwriting newer filters?

## Infrastructure and honest limitations

41. Explain the Docker build stages, network, health checks, and data volume.
42. What runs in GitHub Actions, and which results have actually been observed?
43. Why must a `VITE_` variable never contain the JWT signing secret?
44. What would you change before exposing the application publicly?
45. If an organization became very busy, which lock or query would you investigate first?
46. How would you plan for a million users without pretending this version has been tested at that scale?
47. What would justify Redis, and what evidence would you gather first?

## Self-review rubric

For each answer, identify the code path, explain the reason, name a trade-off, and cite a test or an acknowledged gap. A good answer can say “this is not implemented” and then explain a sensible next step. Do not convert hypothetical scale or planned deployment into resume achievements.

Resume bullet drafting remains gated on reviewing the actual verification record, particularly the unexecuted container and hosted CI checks. No usage, latency, availability, or scalability numbers are supplied by this project.
