package com.example.workflow.organization;

import com.example.workflow.auth.Actor;
import com.example.workflow.common.ApiException;
import com.example.workflow.project.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.*;

/** Called inside service transactions. Organization writes serialize membership and resource changes. */
@Component
public class OrganizationAccess {
    private final OrganizationRepository organizations;
    private final MembershipRepository members;
    private final ProjectRepository projects;
    private final Actor actor;
    private final EntityManager em;
    public OrganizationAccess(OrganizationRepository organizations, MembershipRepository members,
        ProjectRepository projects, Actor actor, EntityManager em) {
        this.organizations=organizations; this.members=members; this.projects=projects; this.actor=actor; this.em=em;
    }
    public Organization organization(UUID id, boolean write) {
        var org = (write ? organizations.lock(id) : organizations.findById(id)).orElseThrow(() -> ApiException.missing("Organization"));
        role(id);
        return org;
    }
    public Role role(UUID organizationId) {
        return members.findByOrganizationIdAndUserId(organizationId, actor.id())
            .filter(m -> m.getStatus() == MembershipStatus.ACTIVE).map(Membership::getRole).orElseThrow(ApiException::forbidden);
    }
    public void require(UUID organizationId, Role... allowed) {
        if (!Arrays.asList(allowed).contains(role(organizationId))) throw ApiException.forbidden();
    }
    public Project project(UUID id, boolean write) {
        var project = projects.findById(id).orElseThrow(() -> ApiException.missing("Project"));
        organization(project.getOrganizationId(), write);
        if (write) em.refresh(project);
        return project;
    }
    public void writable(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) throw ApiException.conflict("Restore the archived project before making changes");
    }
    public void activeAssignee(UUID org, UUID user) {
        if (user != null && members.findByOrganizationIdAndUserId(org, user)
            .filter(m -> m.getStatus() == MembershipStatus.ACTIVE).isEmpty()) throw ApiException.invalid("Assignee must be an active organization member");
    }
}
