package com.example.workflow.organization;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.common.ApiException;
import com.example.workflow.task.TaskRepository;
import com.example.workflow.project.ProjectRepository;
import com.example.workflow.user.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MembershipService {
    private final MembershipRepository members;
    private final OrganizationAccess access;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final ActivityService activity;
    public MembershipService(MembershipRepository members, OrganizationAccess access, UserRepository users, TaskRepository tasks, ProjectRepository projects, ActivityService activity) {
        this.members=members; this.access=access; this.users=users; this.tasks=tasks; this.projects=projects; this.activity=activity;
    }
    public record Add(@NotBlank @Email @Size(max=254) String email, @NotNull Role role) {}
    public record Change(@NotNull Role role) {}
    public record View(UUID userId, String displayName, Role role, MembershipStatus status) {}
    @Transactional(readOnly=true)
    public List<View> list(UUID org) {
        access.organization(org,false);
        var membership=members.findByOrganizationIdAndStatusOrderByCreatedAt(org,MembershipStatus.ACTIVE);
        var people=users.findAllById(membership.stream().map(Membership::getUserId).toList()).stream().collect(Collectors.toMap(AppUser::getId,Function.identity()));
        return membership.stream().map(m -> view(m,people.get(m.getUserId()))).toList();
    }
    @Transactional
    public View add(UUID org, Add request) {
        access.organization(org,true); checkManage(access.role(org), null, request.role());
        var user=users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> ApiException.missing("Registered user"));
        var existing=members.findByOrganizationIdAndUserId(org,user.getId());
        if (existing.filter(m -> m.getStatus()==MembershipStatus.ACTIVE).isPresent()) throw ApiException.conflict("User is already a member");
        var member=existing.orElseGet(Membership::new); member.setOrganizationId(org); member.setUserId(user.getId());
        member.setRole(request.role()); member.setStatus(MembershipStatus.ACTIVE); members.save(member);
        activity.record(org,null,"MEMBERSHIP",user.getId(),"MEMBER_ADDED",null,request.role());
        return view(member,user);
    }
    @Transactional
    public View change(UUID org, UUID user, Change request) {
        access.organization(org,true); var member=active(org,user); checkManage(access.role(org),member.getRole(),request.role());
        activity.record(org,null,"MEMBERSHIP",user,"ROLE_CHANGED",member.getRole(),request.role()); member.setRole(request.role());
        return view(member,users.findById(user).orElseThrow());
    }
    @Transactional
    public void remove(UUID org, UUID user) {
        access.organization(org,true); var member=active(org,user); checkManage(access.role(org),member.getRole(),null);
        member.setStatus(MembershipStatus.REMOVED);
        var projectIds=projects.findByOrganizationIdOrderByCreatedAtDesc(org).stream().map(p -> p.getId()).toList();
        // Process bounded batches; the organization lock prevents concurrent assignment during removal.
        if (!projectIds.isEmpty()) {
            while (true) {
                var assigned=tasks.findByProjectIdInAndAssigneeId(projectIds,user,PageRequest.of(0,100));
                if (assigned.isEmpty()) break;
                assigned.forEach(task -> { task.setAssigneeId(null); activity.record(org,task.getProjectId(),"TASK",task.getId(),"ASSIGNEE_CHANGED",user,null); });
                tasks.flush();
            }
        }
        activity.record(org,null,"MEMBERSHIP",user,"MEMBER_REMOVED",member.getRole(),null);
    }
    public static void checkManage(Role actor, Role target, Role desired) {
        if (target==Role.OWNER || desired==Role.OWNER) throw ApiException.forbidden();
        if (actor==Role.OWNER) return;
        if (actor!=Role.ADMIN || target==Role.ADMIN || desired==Role.ADMIN) throw ApiException.forbidden();
    }
    private Membership active(UUID org, UUID user) {
        return members.findByOrganizationIdAndUserId(org,user).filter(m -> m.getStatus()==MembershipStatus.ACTIVE).orElseThrow(() -> ApiException.missing("Member"));
    }
    private View view(Membership m, AppUser u) { return new View(u.getId(),u.getDisplayName(),m.getRole(),m.getStatus()); }
}
