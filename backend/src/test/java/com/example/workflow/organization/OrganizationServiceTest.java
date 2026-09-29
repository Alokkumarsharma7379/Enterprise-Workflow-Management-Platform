package com.example.workflow.organization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.auth.Actor;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationServiceTest {

  @Test
  void creatorBecomesOwner() {
    var organizations = mock(OrganizationRepository.class);
    var members = mock(MembershipRepository.class);
    var access = mock(OrganizationAccess.class);
    var actor = mock(Actor.class);
    var activity = mock(ActivityService.class);
    UUID user = UUID.randomUUID();
    when(actor.id()).thenReturn(user);
    var service = new OrganizationService(
      organizations,
      members,
      access,
      actor,
      activity
    );
    var result = service.create(new OrganizationService.Create("Engineering"));
    assertEquals(Role.OWNER, result.role());
    verify(members).save(
      argThat(
        m ->
          m.getUserId().equals(user) &&
          m.getOrganizationId().equals(result.id()) &&
          m.getRole() == Role.OWNER &&
          m.getStatus() == MembershipStatus.ACTIVE
      )
    );
    verify(activity).record(
      eq(result.id()),
      isNull(),
      eq("ORGANIZATION"),
      eq(result.id()),
      eq("ORGANIZATION_CREATED"),
      isNull(),
      eq("Engineering")
    );
  }
}
