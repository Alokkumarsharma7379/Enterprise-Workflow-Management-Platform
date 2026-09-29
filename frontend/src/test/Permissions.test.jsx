import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import ProjectPage from "../pages/ProjectPage";
import { canEditTask, canAdmin } from "../utils/permissions";
vi.mock("../api", () => ({
  projectsApi: {
    get: vi.fn(async () => ({
      id: "p1",
      organizationId: "o1",
      name: "Private project",
      key: "PR",
      status: "ACTIVE",
      role: "MEMBER",
    })),
    statistics: vi.fn(async () => ({
      totalTasks: 0,
      byStatus: {
        TODO: 0,
        IN_PROGRESS: 0,
        IN_REVIEW: 0,
        DONE: 0,
        CANCELLED: 0,
      },
    })),
  },
  organizationsApi: { members: vi.fn(async () => []) },
  labelsApi: { list: vi.fn(async () => []) },
  tasksApi: {
    list: vi.fn(async () => ({
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
    })),
  },
}));
it("hides administrative and task creation controls from members", async () => {
  render(
    <MemoryRouter initialEntries={["/projects/p1"]}>
      <Routes>
        <Route path="/projects/:id" element={<ProjectPage />} />
      </Routes>
    </MemoryRouter>,
  );
  expect(await screen.findByText("Private project")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "New task" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Settings" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Manage labels" }),
  ).not.toBeInTheDocument();
});
it("limits member task editing to their assignments", () => {
  expect(canEditTask("MEMBER", { id: "a" }, { assignee: { id: "a" } })).toBe(
    true,
  );
  expect(canEditTask("MEMBER", { id: "a" }, { assignee: { id: "b" } })).toBe(
    false,
  );
  expect(canEditTask("MANAGER", { id: "a" }, { assignee: null })).toBe(true);
  expect(canAdmin("MANAGER")).toBe(false);
});
