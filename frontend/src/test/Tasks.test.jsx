import { it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import TaskCard from "../features/TaskCard";
import TaskForm from "../features/TaskForm";
import { tasksApi } from "../api";
vi.mock("../api", () => ({ tasksApi: { create: vi.fn() } }));
beforeEach(() => vi.resetAllMocks());
it("renders a task with priority, labels, and a detail link", () => {
  render(
    <MemoryRouter>
      <TaskCard
        task={{
          id: "task-1",
          identifier: "PLAT-1",
          title: "Implement authentication",
          priority: "HIGH",
          dueDate: "2026-10-01",
          labels: [{ id: "label-1", name: "backend", color: "#123456" }],
          assignee: { id: "user-1", displayName: "Alok" },
        }}
      />
    </MemoryRouter>,
  );
  expect(screen.getByRole("link")).toHaveAttribute("href", "/tasks/task-1");
  expect(screen.getByText("Implement authentication")).toBeVisible();
  expect(screen.getByText("High")).toBeVisible();
  expect(screen.getByText("backend")).toBeVisible();
});
it("creates an unassigned task with nullable due date", async () => {
  tasksApi.create.mockResolvedValue({ id: "new-task" });
  const done = vi.fn();
  render(<TaskForm projectId="project-1" members={[]} onCreated={done} />);
  await userEvent.type(
    screen.getByLabelText("Task title"),
    "Build a useful task board",
  );
  await userEvent.click(screen.getByRole("button", { name: "Create task" }));
  await waitFor(() =>
    expect(tasksApi.create).toHaveBeenCalledWith("project-1", {
      title: "Build a useful task board",
      description: "",
      priority: "MEDIUM",
      assigneeId: null,
      dueDate: null,
    }),
  );
  expect(done).toHaveBeenCalledWith({ id: "new-task" });
});
it("preserves input and displays validation errors", async () => {
  tasksApi.create.mockRejectedValue(
    new Error("Assignee must be an active organization member"),
  );
  render(<TaskForm projectId="project-1" members={[]} onCreated={vi.fn()} />);
  await userEvent.type(screen.getByLabelText("Task title"), "Keep this title");
  await userEvent.click(screen.getByRole("button", { name: "Create task" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Assignee must be an active",
  );
  expect(screen.getByLabelText("Task title")).toHaveValue("Keep this title");
});
