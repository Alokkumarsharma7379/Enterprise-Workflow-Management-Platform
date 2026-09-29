import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 60000,
  expect: { timeout: 10000 },
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:5173",
    headless: true,
    ...(process.env.E2E_CHROME_PATH
      ? { launchOptions: { executablePath: process.env.E2E_CHROME_PATH } }
      : {}),
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  reporter: "list",
});
