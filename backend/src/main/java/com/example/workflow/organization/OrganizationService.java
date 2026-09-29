package com.example.workflow.organization;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.auth.Actor;
import com.example.workflow.common.Patch;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

  private final OrganizationRepository organizations;
  private final MembershipRepository members;
  private final OrganizationAccess access;
  private final Actor actor;
  private final ActivityService activity;

  public OrganizationService(
    OrganizationRepository organizations,
    MembershipRepository members,
    OrganizationAccess access,
    Actor actor,
    ActivityService activity
  ) {
    this.organizations = organizations;
    this.members = members;
    this.access = access;
    this.actor = actor;
    this.activity = activity;
  }

  public record Create(@NotBlank @Size(max = 120) String name) {}

  public record View(UUID id, String name, Role role, long version) {}

  @Transactional
  public View create(Create request) {
    var org = new Organization();
    org.setName(request.name().trim());
    org.setCreatedBy(actor.id());
    organizations.save(org);
    var member = new Membership();
    member.setOrganizationId(org.getId());
    member.setUserId(actor.id());
    member.setRole(Role.OWNER);
    member.setStatus(MembershipStatus.ACTIVE);
    members.save(member);
    activity.record(
      org.getId(),
      null,
      "ORGANIZATION",
      org.getId(),
      "ORGANIZATION_CREATED",
      null,
      org.getName()
    );
    return new View(org.getId(), org.getName(), Role.OWNER, org.getVersion());
  }

  @Transactional(readOnly = true)
  public List<View> list() {
    var roles = members
      .findByUserIdAndStatus(actor.id(), MembershipStatus.ACTIVE)
      .stream()
      .collect(
        Collectors.toMap(Membership::getOrganizationId, Membership::getRole)
      );
    return organizations
      .accessible(actor.id())
      .stream()
      .map(o ->
        new View(o.getId(), o.getName(), roles.get(o.getId()), o.getVersion())
      )
      .toList();
  }

  @Transactional(readOnly = true)
  public View get(UUID id) {
    var org = access.organization(id, false);
    return new View(id, org.getName(), access.role(id), org.getVersion());
  }

  @Transactional
  public View update(UUID id, JsonNode json) {
    var org = access.organization(id, true);
    access.require(id, Role.OWNER);
    var patch = new Patch(json, "name", "version");
    patch.version(org.getVersion());
    if (patch.has("name")) {
      String name = patch.text("name", 120, false);
      activity.record(
        id,
        null,
        "ORGANIZATION",
        id,
        "ORGANIZATION_RENAMED",
        org.getName(),
        name
      );
      org.setName(name);
    }
    organizations.flush();
    return new View(id, org.getName(), Role.OWNER, org.getVersion());
  }

  @Transactional
  public void delete(UUID id) {
    var org = access.organization(id, true);
    access.require(id, Role.OWNER);
    organizations.delete(org);
  }
}
