package com.example.workflow.organization;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

  private final OrganizationService organizations;
  private final MembershipService members;

  public OrganizationController(
    OrganizationService organizations,
    MembershipService members
  ) {
    this.organizations = organizations;
    this.members = members;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  OrganizationService.View create(
    @Valid @RequestBody OrganizationService.Create request
  ) {
    return organizations.create(request);
  }

  @GetMapping
  List<OrganizationService.View> list() {
    return organizations.list();
  }

  @GetMapping("/{id}")
  OrganizationService.View get(@PathVariable UUID id) {
    return organizations.get(id);
  }

  @PatchMapping("/{id}")
  OrganizationService.View update(
    @PathVariable UUID id,
    @RequestBody JsonNode patch
  ) {
    return organizations.update(id, patch);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id) {
    organizations.delete(id);
  }

  @GetMapping("/{id}/members")
  List<MembershipService.View> members(@PathVariable UUID id) {
    return members.list(id);
  }

  @PostMapping("/{id}/members")
  @ResponseStatus(HttpStatus.CREATED)
  MembershipService.View add(
    @PathVariable UUID id,
    @Valid @RequestBody MembershipService.Add request
  ) {
    return members.add(id, request);
  }

  @PatchMapping("/{id}/members/{userId}")
  MembershipService.View change(
    @PathVariable UUID id,
    @PathVariable UUID userId,
    @Valid @RequestBody MembershipService.Change request
  ) {
    return members.change(id, userId, request);
  }

  @DeleteMapping("/{id}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void remove(@PathVariable UUID id, @PathVariable UUID userId) {
    members.remove(id, userId);
  }
}
