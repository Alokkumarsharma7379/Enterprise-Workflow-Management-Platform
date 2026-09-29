package com.example.workflow.label;

import com.example.workflow.activity.ActivityService;
import com.example.workflow.common.*;
import com.example.workflow.organization.*;
import com.example.workflow.project.Project;
import com.example.workflow.task.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class LabelService {
    private final LabelRepository labels;
    private final TaskLabelRepository links;
    private final TaskRepository tasks;
    private final OrganizationAccess access;
    private final ActivityService activity;
    public LabelService(LabelRepository labels,TaskLabelRepository links,TaskRepository tasks,OrganizationAccess access,ActivityService activity) {
        this.labels=labels; this.links=links; this.tasks=tasks; this.access=access; this.activity=activity;
    }
    public record Create(@NotBlank @Size(max=50) String name,@NotNull @Pattern(regexp="#[0-9a-fA-F]{6}") String color) {}
    public record View(UUID id,String name,String color) {}
    @Transactional(readOnly=true)
    public List<View> list(UUID projectId) { access.project(projectId,false); return labels.findByProjectIdOrderByName(projectId).stream().map(this::view).toList(); }
    @Transactional
    public View create(UUID projectId,Create request) {
        var p=access.project(projectId,true); manage(p); var label=new Label(); label.setProjectId(projectId); label.setName(request.name().trim().toLowerCase(Locale.ROOT)); label.setColor(request.color()); labels.saveAndFlush(label);
        activity.record(p.getOrganizationId(),p.getId(),"LABEL",label.getId(),"LABEL_CREATED",null,label.getName()); return view(label);
    }
    @Transactional
    public View update(UUID id,JsonNode json) {
        var label=find(id); var p=access.project(label.getProjectId(),true); manage(p); var patch=new Patch(json,"name","color");
        if (patch.has("name")) label.setName(patch.text("name",50,false).toLowerCase(Locale.ROOT));
        if (patch.has("color")) { String color=patch.text("color",7,false); if (!color.matches("#[0-9a-fA-F]{6}")) throw ApiException.invalid("Invalid label color"); label.setColor(color); }
        labels.flush(); activity.record(p.getOrganizationId(),p.getId(),"LABEL",id,"LABEL_UPDATED",null,label.getName()); return view(label);
    }
    @Transactional
    public void delete(UUID id) {
        var label=find(id); var p=access.project(label.getProjectId(),true); manage(p); labels.delete(label);
        activity.record(p.getOrganizationId(),p.getId(),"LABEL",id,"LABEL_DELETED",label.getName(),null);
    }
    @Transactional
    public void attach(UUID taskId,UUID labelId,boolean add) {
        var task=tasks.findById(taskId).orElseThrow(() -> ApiException.missing("Task")); var p=access.project(task.getProjectId(),true); manage(p);
        var label=find(labelId); if (!label.getProjectId().equals(p.getId())) throw ApiException.invalid("Label must belong to the same project");
        var id=new TaskLabelId(taskId,labelId); boolean exists=links.existsById(id);
        if (add && !exists) links.save(new TaskLabel(taskId,labelId,p.getId()));
        else if (!add && exists) links.deleteById(id);
        else return;
        activity.record(p.getOrganizationId(),p.getId(),"TASK",taskId,add?"LABEL_ATTACHED":"LABEL_REMOVED",add?null:label.getName(),add?label.getName():null);
    }
    private Label find(UUID id) { return labels.findById(id).orElseThrow(() -> ApiException.missing("Label")); }
    private void manage(Project p) { access.require(p.getOrganizationId(),Role.OWNER,Role.ADMIN,Role.MANAGER); access.writable(p); }
    private View view(Label label) { return new View(label.getId(),label.getName(),label.getColor()); }
}
