package com.example.workflow.label;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "labels")
public class Label {
    @Id
    private UUID id = UUID.randomUUID();
    private UUID projectId;
    private String name;
    private String color;

    public UUID getId() { return id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
