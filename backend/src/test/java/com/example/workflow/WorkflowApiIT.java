package com.example.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WorkflowApiIT.TestPasswords.class)
class WorkflowApiIT {

  static PostgreSQLContainer<?> postgres;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String url = System.getenv("TEST_DB_URL");
    if (url == null) {
      postgres = new PostgreSQLContainer<>(
        "postgres:18-alpine"
      ).withDatabaseName("workflow_test");
      postgres.start();
      properties.add("spring.datasource.url", postgres::getJdbcUrl);
      properties.add("spring.datasource.username", postgres::getUsername);
      properties.add("spring.datasource.password", postgres::getPassword);
    } else {
      if (
        !url.matches("jdbc:postgresql://[^/]+/[^?]*_test(?:\\?.*)?")
      ) throw new IllegalStateException(
        "TEST_DB_URL must point to a database ending in _test"
      );
      properties.add("spring.datasource.url", () -> url);
      properties.add("spring.datasource.username", () ->
        System.getenv().getOrDefault("TEST_DB_USERNAME", "workflow")
      );
      properties.add("spring.datasource.password", () ->
        System.getenv().getOrDefault("TEST_DB_PASSWORD", "")
      );
    }
    properties.add(
      "app.jwt-secret",
      () -> "test-only-secret-at-least-thirty-two-bytes-long"
    );
    properties.add(
      "spring.jpa.properties.hibernate.generate_statistics",
      () -> true
    );
    properties.add("logging.level.org.hibernate.stat", () -> "OFF");
    properties.add(
      "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener",
      () -> "OFF"
    );
  }

  @TestConfiguration
  static class TestPasswords {

    @Bean
    @Primary
    PasswordEncoder fastTestEncoder() {
      return new BCryptPasswordEncoder(4);
    }
  }

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  EntityManagerFactory emf;

  @Autowired
  JwtEncoder jwtEncoder;

  record Account(String token, String id, String email, Cookie refresh) {}

  Account owner, admin, manager, member, other;
  String org, project;

  @BeforeEach
  void setup() throws Exception {
    jdbc.execute("TRUNCATE TABLE users CASCADE");
    owner = register("owner");
    admin = register("admin");
    manager = register("manager");
    member = register("member");
    other = register("other");
    org = body(
      send(
        owner,
        post("/api/organizations"),
        Map.of("name", "Engineering"),
        201
      )
    )
      .get("id")
      .asText();
    add(admin, "ADMIN");
    add(manager, "MANAGER");
    add(member, "MEMBER");
    project = body(
      send(
        owner,
        post("/api/organizations/" + org + "/projects"),
        Map.of("name", "Platform", "key", "PLAT"),
        201
      )
    )
      .get("id")
      .asText();
  }

  @Test
  void registrationLoginAndAuthentication() throws Exception {
    var login = body(
      send(
        null,
        post("/api/auth/login").with(csrf()),
        Map.of(
          "email",
          owner.email().toUpperCase(),
          "password",
          "correct-password-123"
        ),
        200
      )
    );
    assertTrue(login.hasNonNull("accessToken"));
    assertFalse(login.get("user").has("passwordHash"));
    send(owner, get("/api/auth/me"), null, 200);
    send(null, get("/api/organizations"), null, 401);
    send(
      null,
      get("/api/organizations").header("Authorization", "Bearer invalid"),
      null,
      401
    );
    send(
      null,
      post("/api/auth/login").with(csrf()),
      Map.of("email", owner.email(), "password", "wrong"),
      401
    );
    String hash = jdbc.queryForObject(
      "select password_hash from users where id=?",
      String.class,
      UUID.fromString(owner.id())
    );
    assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
    assertNotEquals("correct-password-123", hash);
  }

  @Test
  void csrfRequiredForCookieAuthentication() throws Exception {
    send(null, post("/api/auth/refresh").cookie(owner.refresh()), null, 403);
    send(null, get("/api/auth/csrf"), null, 200);
  }

  @Test
  void validatesJwtExpiryIssuerAudienceAndSignature() throws Exception {
    for (String scenario : List.of("expired", "issuer", "audience")) {
      var claims = JwtClaimsSet.builder()
        .subject(owner.id())
        .issuer(scenario.equals("issuer") ? "other" : "workflow")
        .audience(
          List.of(scenario.equals("audience") ? "other" : "workflow-api")
        )
        .issuedAt(Instant.now().minusSeconds(3600))
        .expiresAt(
          Instant.now().plusSeconds(scenario.equals("expired") ? -600 : 600)
        )
        .build();
      String token = jwtEncoder
        .encode(
          JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims
          )
        )
        .getTokenValue();
      send(
        null,
        get("/api/organizations").header("Authorization", "Bearer " + token),
        null,
        401
      );
    }
    String[] parts = owner.token().split("\\.");
    String signature = parts[2];
    signature =
      (signature.charAt(0) == 'a' ? "b" : "a") + signature.substring(1);
    send(
      null,
      get("/api/organizations").header(
        "Authorization",
        "Bearer " + parts[0] + "." + parts[1] + "." + signature
      ),
      null,
      401
    );
  }

  @Test
  void rejectsInvalidRegistrationDuplicateEmailAndMissingRequiredFields()
    throws Exception {
    send(
      null,
      post("/api/auth/register").with(csrf()),
      Map.of("email", "not-email", "password", "short", "displayName", ""),
      400
    );
    send(
      null,
      post("/api/auth/register").with(csrf()),
      Map.of(
        "email",
        owner.email().toUpperCase(),
        "password",
        "correct-password-123",
        "displayName",
        "Duplicate"
      ),
      409
    );
    send(
      owner,
      post("/api/projects/" + project + "/tasks"),
      Map.of("title", " ", "priority", "MEDIUM"),
      400
    );
    send(owner, get("/api/does-not-exist"), null, 404);
    send(owner, put("/api/organizations"), null, 405);
  }

  @Test
  void prioritySortIsBusinessOrderAndSearchTreatsWildcardLiterally()
    throws Exception {
    var ids = new HashMap<String, String>();
    for (String priority : List.of("LOW", "HIGH", "CRITICAL", "MEDIUM")) {
      var task = body(
        send(
          owner,
          post("/api/projects/" + project + "/tasks"),
          Map.of("title", priority + " 100% coverage", "priority", priority),
          201
        )
      );
      ids.put(priority, task.get("id").asText());
    }
    var result = body(
      send(
        member,
        get("/api/projects/" + project + "/tasks").param(
          "sort",
          "priority,desc"
        ),
        null,
        200
      )
    );
    assertEquals(
      "CRITICAL",
      result.get("content").get(0).get("priority").asText()
    );
    assertEquals("HIGH", result.get("content").get(1).get("priority").asText());
    assertEquals("LOW", result.get("content").get(3).get("priority").asText());
    createTask(owner, "No percent symbol", null);
    result = body(
      send(
        member,
        get("/api/projects/" + project + "/tasks").param("search", "%"),
        null,
        200
      )
    );
    assertEquals(4, result.get("totalElements").asInt());
  }

  @Test
  void refreshRotationReuseRevokesFamilyAndLogoutRevokes() throws Exception {
    var result = send(
      null,
      post("/api/auth/refresh").cookie(owner.refresh()).with(csrf()),
      null,
      200
    );
    Cookie rotated = result.getResponse().getCookie("refresh_token");
    assertNotNull(rotated);
    assertNotEquals(owner.refresh().getValue(), rotated.getValue());
    send(
      null,
      post("/api/auth/refresh").cookie(owner.refresh()).with(csrf()),
      null,
      401
    );
    send(
      null,
      post("/api/auth/refresh").cookie(rotated).with(csrf()),
      null,
      401
    );
    send(
      null,
      post("/api/auth/logout").cookie(member.refresh()).with(csrf()),
      null,
      204
    );
    send(
      null,
      post("/api/auth/refresh").cookie(member.refresh()).with(csrf()),
      null,
      401
    );
  }

  @Test
  void memberCannotDeleteOrganizationOrPromoteAnotherUser() throws Exception {
    send(member, delete("/api/organizations/" + org), null, 403);
    send(
      member,
      patch("/api/organizations/" + org + "/members/" + manager.id()),
      Map.of("role", "ADMIN"),
      403
    );
    send(
      admin,
      patch("/api/organizations/" + org + "/members/" + member.id()),
      Map.of("role", "ADMIN"),
      403
    );
    send(
      owner,
      delete("/api/organizations/" + org + "/members/" + owner.id()),
      null,
      403
    );
  }

  @Test
  void adminCreatesProjectsAndManagerManagesTasks() throws Exception {
    send(
      admin,
      post("/api/organizations/" + org + "/projects"),
      Map.of("name", "Admin project", "key", "ADMIN"),
      201
    );
    JsonNode task = createTask(manager, "Manager task", member.id());
    send(
      manager,
      patch("/api/tasks/" + task.get("id").asText()),
      Map.of("version", 0, "priority", "CRITICAL"),
      200
    );
    send(
      member,
      post("/api/projects/" + project + "/tasks"),
      Map.of("title", "Not permitted", "priority", "LOW"),
      403
    );
  }

  @Test
  void outsidersCannotAccessAnyResourceByChangingIds() throws Exception {
    JsonNode task = createTask(owner, "Secret task", null);
    String id = task.get("id").asText();
    String comment = body(
      send(
        owner,
        post("/api/tasks/" + id + "/comments"),
        Map.of("body", "Internal"),
        201
      )
    )
      .get("id")
      .asText();
    for (String path : List.of(
      "/api/organizations/" + org,
      "/api/organizations/" + org + "/members",
      "/api/projects/" + project,
      "/api/projects/" + project + "/tasks",
      "/api/projects/" + project + "/statistics",
      "/api/projects/" + project + "/labels",
      "/api/projects/" + project + "/activity",
      "/api/tasks/" + id,
      "/api/tasks/" + id + "/comments",
      "/api/tasks/" + id + "/activity"
    ))
      send(other, get(path), null, 403);
    send(
      other,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "title", "Stolen"),
      403
    );
    send(
      other,
      patch("/api/comments/" + comment),
      Map.of("version", 0, "body", "Stolen"),
      403
    );
  }

  @Test
  void commentAuthorCanEditButOtherMemberAndAdminCannotRewrite()
    throws Exception {
    String id = createTask(owner, "Discussion", null).get("id").asText();
    String comment = body(
      send(
        member,
        post("/api/tasks/" + id + "/comments"),
        Map.of("body", "Original"),
        201
      )
    )
      .get("id")
      .asText();
    send(
      manager,
      patch("/api/comments/" + comment),
      Map.of("version", 0, "body", "Other"),
      403
    );
    send(
      admin,
      patch("/api/comments/" + comment),
      Map.of("version", 0, "body", "Other"),
      403
    );
    send(
      member,
      patch("/api/comments/" + comment),
      Map.of("version", 0, "body", "Edited"),
      200
    );
    send(admin, delete("/api/comments/" + comment), null, 204);
  }

  @Test
  void assignedMemberChangesStatusButCannotChangePriority() throws Exception {
    String id = createTask(manager, "Assigned", member.id()).get("id").asText();
    send(
      member,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "priority", "HIGH"),
      403
    );
    send(
      member,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "status", "IN_PROGRESS"),
      200
    );
    send(
      member,
      patch("/api/tasks/" + id),
      Map.of("version", 1, "status", "DONE"),
      409
    );
    send(
      member,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "title", "Stale"),
      409
    );
    send(
      member,
      patch("/api/tasks/" + id),
      Map.of("version", 1, "status", "IN_REVIEW"),
      200
    );
    send(
      member,
      patch("/api/tasks/" + id),
      Map.of("version", 2, "status", "DONE"),
      200
    );
  }

  @Test
  void invalidPatchRollsBackTaskAndActivity() throws Exception {
    String id = createTask(owner, "Original", null).get("id").asText();
    long before = jdbc.queryForObject(
      "select count(*) from activity_logs",
      Long.class
    );
    send(
      owner,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "title", "Changed", "status", "DONE"),
      409
    );
    assertEquals(
      "Original",
      body(send(owner, get("/api/tasks/" + id), null, 200))
        .get("title")
        .asText()
    );
    assertEquals(
      before,
      jdbc.queryForObject("select count(*) from activity_logs", Long.class)
    );
    send(
      owner,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "reporterId", other.id()),
      400
    );
  }

  @Test
  void membershipRemovalUnassignsTasksAndImmediatelyBlocksAccess()
    throws Exception {
    String id = createTask(owner, "Assigned", member.id()).get("id").asText();
    send(
      owner,
      delete("/api/organizations/" + org + "/members/" + member.id()),
      null,
      204
    );
    assertTrue(
      body(send(owner, get("/api/tasks/" + id), null, 200))
        .get("assignee")
        .isNull()
    );
    send(member, get("/api/tasks/" + id), null, 403);
    send(
      owner,
      patch("/api/tasks/" + id),
      Map.of("version", 1, "assigneeId", member.id()),
      400
    );
  }

  @Test
  void filteringPaginationLabelsAndStatisticsUseDatabaseResults()
    throws Exception {
    String id = createTask(owner, "Authentication flow", member.id())
      .get("id")
      .asText();
    createTask(owner, "Other work", null);
    String label = body(
      send(
        owner,
        post("/api/projects/" + project + "/labels"),
        Map.of("name", "backend", "color", "#123456"),
        201
      )
    )
      .get("id")
      .asText();
    send(manager, put("/api/tasks/" + id + "/labels/" + label), null, 204);
    var result = body(
      send(
        member,
        get("/api/projects/" + project + "/tasks")
          .param("search", "AUTH")
          .param("priority", "MEDIUM")
          .param("assigneeId", member.id())
          .param("labelId", label)
          .param("size", "1"),
        null,
        200
      )
    );
    assertEquals(1, result.get("totalElements").asInt());
    assertEquals(id, result.get("content").get(0).get("id").asText());
    assertEquals(
      "backend",
      result.get("content").get(0).get("labels").get(0).get("name").asText()
    );
    var second = body(
      send(
        member,
        get("/api/projects/" + project + "/tasks")
          .param("page", "1")
          .param("size", "1"),
        null,
        200
      )
    );
    assertEquals(1, second.get("content").size());
    var stats = body(
      send(member, get("/api/projects/" + project + "/statistics"), null, 200)
    );
    assertEquals(2, stats.get("totalTasks").asInt());
    assertEquals(2, stats.get("byStatus").get("TODO").asInt());
    send(
      member,
      get("/api/projects/" + project + "/tasks").param("size", "101"),
      null,
      400
    );
    send(
      member,
      get("/api/projects/" + project + "/tasks").param(
        "sort",
        "passwordHash,desc"
      ),
      null,
      400
    );
  }

  @Test
  void rejectsCrossProjectLabelsAndOutsideAssignees() throws Exception {
    String id = createTask(owner, "Task", null).get("id").asText();
    String another = body(
      send(
        owner,
        post("/api/organizations/" + org + "/projects"),
        Map.of("name", "Other", "key", "OTHER"),
        201
      )
    )
      .get("id")
      .asText();
    String label = body(
      send(
        owner,
        post("/api/projects/" + another + "/labels"),
        Map.of("name", "foreign", "color", "#abcdef"),
        201
      )
    )
      .get("id")
      .asText();
    send(owner, put("/api/tasks/" + id + "/labels/" + label), null, 400);
    send(
      owner,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "assigneeId", other.id()),
      400
    );
    assertThrows(
      org.springframework.dao.DataIntegrityViolationException.class,
      () ->
        jdbc.update(
          "insert into task_labels(task_id,label_id,project_id) values (?,?,?)",
          UUID.fromString(id),
          UUID.fromString(label),
          UUID.fromString(project)
        )
    );
  }

  @Test
  void nullablePatchClearsWithoutChangingOmittedFields() throws Exception {
    String id = createTask(owner, "Preserved title", member.id())
      .get("id")
      .asText();
    var values = new HashMap<String, Object>();
    values.put("version", 0);
    values.put("assigneeId", null);
    var result = body(send(owner, patch("/api/tasks/" + id), values, 200));
    assertTrue(result.get("assignee").isNull());
    assertEquals("Preserved title", result.get("title").asText());
  }

  @Test
  void archivedProjectRejectsMutationsButCanBeRestored() throws Exception {
    String id = createTask(owner, "Readonly", null).get("id").asText();
    long version = body(send(owner, get("/api/projects/" + project), null, 200))
      .get("version")
      .asLong();
    var archived = body(
      send(
        admin,
        patch("/api/projects/" + project),
        Map.of("version", version, "status", "ARCHIVED"),
        200
      )
    );
    send(
      manager,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "title", "No"),
      409
    );
    send(
      member,
      post("/api/tasks/" + id + "/comments"),
      Map.of("body", "No"),
      409
    );
    send(member, get("/api/tasks/" + id), null, 200);
    send(
      admin,
      patch("/api/projects/" + project),
      Map.of("version", archived.get("version").asLong(), "status", "ACTIVE"),
      200
    );
    send(
      manager,
      patch("/api/tasks/" + id),
      Map.of("version", 0, "title", "Restored"),
      200
    );
  }

  @Test
  void concurrentCreationAllocatesUniqueTaskNumbers() throws Exception {
    try (var executor = Executors.newFixedThreadPool(4)) {
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        int number = i;
        futures.add(
          executor.submit(() ->
            createTask(manager, "Concurrent " + number, null)
              .get("identifier")
              .asText()
          )
        );
      }
      Set<String> identifiers = new HashSet<>();
      for (var future : futures)
        identifiers.add(future.get(30, TimeUnit.SECONDS));
      assertEquals(8, identifiers.size());
      assertTrue(identifiers.contains("PLAT-8"));
    }
  }

  @Test
  void listingQueryCountDoesNotGrowWithTaskCount() throws Exception {
    createTask(owner, "First", member.id());
    var stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    send(owner, get("/api/projects/" + project + "/tasks"), null, 200);
    long one = stats.getPrepareStatementCount();
    for (int i = 0; i < 12; i++) createTask(owner, "More " + i, member.id());
    stats.clear();
    send(owner, get("/api/projects/" + project + "/tasks"), null, 200);
    long many = stats.getPrepareStatementCount();
    assertEquals(one, many, "Task page queries must stay bounded as rows grow");
    assertTrue(many <= 10, "Unexpected query count: " + many);
  }

  @Test
  void deletionCascadesAndRetainsProjectHistoryAtOrganizationScope()
    throws Exception {
    String id = createTask(owner, "Delete me", null).get("id").asText();
    send(
      member,
      post("/api/tasks/" + id + "/comments"),
      Map.of("body", "A comment"),
      201
    );
    send(admin, delete("/api/projects/" + project), null, 204);
    assertEquals(
      0,
      jdbc.queryForObject("select count(*) from tasks", Integer.class)
    );
    assertEquals(
      0,
      jdbc.queryForObject("select count(*) from comments", Integer.class)
    );
    assertTrue(
      jdbc.queryForObject(
        "select count(*) from activity_logs where action_type='PROJECT_DELETED'",
        Integer.class
      ) > 0
    );
    send(owner, delete("/api/organizations/" + org), null, 204);
    assertEquals(
      0,
      jdbc.queryForObject("select count(*) from activity_logs", Integer.class)
    );
  }

  private Account register(String name) throws Exception {
    String email = name + "@example.test";
    var result = send(
      null,
      post("/api/auth/register").with(csrf()),
      Map.of(
        "email",
        email,
        "password",
        "correct-password-123",
        "displayName",
        name
      ),
      201
    );
    var body = body(result);
    return new Account(
      body.get("accessToken").asText(),
      body.get("user").get("id").asText(),
      email,
      result.getResponse().getCookie("refresh_token")
    );
  }

  private void add(Account account, String role) throws Exception {
    send(
      owner,
      post("/api/organizations/" + org + "/members"),
      Map.of("email", account.email(), "role", role),
      201
    );
  }

  private JsonNode createTask(Account actor, String title, String assignee)
    throws Exception {
    Map<String, Object> values = new HashMap<>();
    values.put("title", title);
    values.put("priority", "MEDIUM");
    if (assignee != null) values.put("assigneeId", assignee);
    return body(
      send(actor, post("/api/projects/" + project + "/tasks"), values, 201)
    );
  }

  private MvcResult send(
    Account account,
    MockHttpServletRequestBuilder request,
    Object body,
    int status
  ) throws Exception {
    if (account != null) request.header(
      "Authorization",
      "Bearer " + account.token()
    );
    if (body != null) request
      .contentType("application/json")
      .content(json.writeValueAsBytes(body));
    return mvc.perform(request).andExpect(status().is(status)).andReturn();
  }

  private JsonNode body(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsByteArray());
  }
}
