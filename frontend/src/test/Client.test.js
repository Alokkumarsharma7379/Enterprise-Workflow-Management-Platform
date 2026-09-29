import { beforeEach, afterEach, it, expect, vi } from "vitest";
let client;
beforeEach(async () => {
  vi.resetModules();
  client = await import("../api/client");
});
afterEach(() => vi.unstubAllGlobals());
const response = (status, body) => ({
  status,
  ok: status >= 200 && status < 300,
  json: async () => body,
});
it("deduplicates refresh when simultaneous requests return 401", async () => {
  let old = 0,
    refreshes = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url, options) => {
      if (url.endsWith("/auth/csrf")) return response(200, { token: "csrf" });
      if (url.endsWith("/auth/refresh")) {
        refreshes++;
        await new Promise((resolve) => setTimeout(resolve, 10));
        return response(200, { accessToken: "new", user: { id: "1" } });
      }
      if (options.headers.Authorization === "Bearer old") {
        old++;
        return response(401, {});
      }
      return response(200, { ok: true });
    }),
  );
  client.setSession({ accessToken: "old", user: { id: "1" } });
  const results = await Promise.all([
    client.request("/organizations"),
    client.request("/projects/p1"),
  ]);
  expect(old).toBe(2);
  expect(refreshes).toBe(1);
  expect(results).toEqual([{ ok: true }, { ok: true }]);
});
it("does not recursively refresh after a second 401", async () => {
  let refreshes = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url) => {
      if (url.endsWith("/auth/csrf")) return response(200, { token: "csrf" });
      if (url.endsWith("/auth/refresh")) {
        refreshes++;
        return response(200, { accessToken: "new", user: { id: "1" } });
      }
      return response(401, { message: "Rejected" });
    }),
  );
  await expect(client.request("/organizations")).rejects.toMatchObject({
    status: 401,
  });
  expect(refreshes).toBe(1);
});
