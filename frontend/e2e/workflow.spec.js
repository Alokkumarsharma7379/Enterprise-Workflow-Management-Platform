import { test, expect } from "@playwright/test";
import { mkdir } from "node:fs/promises";
test("register, create workspace and project, work a task, refresh, and log out", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  const unique = Date.now();
  await page.goto("/register");
  await page.getByLabel("Full name").fill("Workflow Tester");
  await page
    .getByLabel("Email address")
    .fill(`workflow-${unique}@example.test`);
  await page
    .getByLabel("Password", { exact: true })
    .fill("test-only-password-123");
  await page.getByRole("button", { name: "Create account" }).click();
  await expect(
    page.getByRole("heading", { name: "Hello, Workflow." }),
  ).toBeVisible();
  await page.getByRole("button", { name: "New organization" }).click();
  await page.getByLabel("Organization name").fill(`Engineering ${unique}`);
  await page
    .getByRole("button", { name: "Create organization", exact: true })
    .click();
  await page
    .getByRole("link")
    .filter({
      has: page.getByRole("heading", { name: `Engineering ${unique}` }),
    })
    .click();
  await page.getByRole("button", { name: "New project" }).click();
  await page.getByLabel("Project name").fill("Platform engineering");
  await page.getByLabel("Project key").fill("PLAT");
  await page
    .getByLabel("Description", { exact: true })
    .fill("A real browser test of the complete workflow.");
  await page
    .getByRole("button", { name: "Create project", exact: true })
    .click();
  await page
    .getByRole("link")
    .filter({
      has: page.getByRole("heading", { name: "Platform engineering" }),
    })
    .click();
  await page.getByRole("button", { name: "New task" }).click();
  await page.getByLabel("Task title").fill("Implement a secure login");
  await page
    .getByLabel("Description", { exact: true })
    .fill("Validate tokens and permissions.");
  await page.getByLabel("Priority", { exact: true }).selectOption("HIGH");
  await page
    .getByLabel("Assignee", { exact: true })
    .selectOption({ label: "Workflow Tester" });
  await page.getByRole("button", { name: "Create task", exact: true }).click();
  await expect(page.getByText("1 matching tasks")).toBeVisible();
  if (process.env.CAPTURE_SCREENSHOTS) {
    await mkdir("../docs/screenshots", { recursive: true });
    await page.screenshot({
      path: "../docs/screenshots/project-board.png",
      fullPage: true,
    });
  }
  await page
    .getByRole("link")
    .filter({
      has: page.getByRole("heading", { name: "Implement a secure login" }),
    })
    .click();
  await page.getByLabel("Task status").selectOption("IN_PROGRESS");
  await expect(page.getByLabel("Task status")).toHaveValue("IN_PROGRESS");
  await expect(page.getByLabel("Task status")).toBeEnabled();
  await page.getByLabel("Add a comment").fill("Ready for the first review.");
  await page.getByRole("button", { name: "Post comment" }).click();
  await expect(page.getByText("Ready for the first review.")).toBeVisible();
  if (process.env.CAPTURE_SCREENSHOTS)
    await page.screenshot({
      path: "../docs/screenshots/task-details.png",
      fullPage: true,
    });
  await page.getByRole("button", { name: "Activity", exact: true }).click();
  await expect(page.getByText(/Status changed/)).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Implement a secure login" }),
  ).toBeVisible();
  await expect(page.getByLabel("Task status")).toHaveValue("IN_PROGRESS");
  await page.getByRole("button", { name: "Log out" }).click();
  await expect(
    page.getByRole("heading", { name: "Welcome back" }),
  ).toBeVisible();
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});
