const base = import.meta.env.VITE_API_URL || "/api";
let accessToken = null;
let refreshPromise = null;
let csrfPromise = null;
let onSession = () => {};

export class ApiError extends Error {
  constructor(message, status, fields = {}) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}
export function configureSession(callback) {
  onSession = callback;
}
export function setSession(session) {
  accessToken = session?.accessToken || null;
  onSession(session?.user || null);
}

async function csrf() {
  if (!csrfPromise) {
    csrfPromise = fetch(`${base}/auth/csrf`, { credentials: "include" })
      .then(async (response) => {
        if (!response.ok)
          throw new ApiError(
            "Could not initialize secure authentication",
            response.status,
          );
        return (await response.json()).token;
      })
      .finally(() => {
        // Authentication can clear the CSRF cookie. Cache only an in-flight
        // bootstrap, never a token across authentication state changes.
        csrfPromise = null;
      });
  }
  return csrfPromise;
}

export async function refreshSession() {
  if (!refreshPromise) {
    const rotate = async () => {
      // Serialize refresh across tabs when Web Locks is available.
      const session = await request("/auth/refresh", { method: "POST" }, false);
      setSession(session);
      return session;
    };
    refreshPromise = (
      navigator.locks
        ? navigator.locks.request("workflow-refresh", rotate)
        : rotate()
    )
      .catch((error) => {
        if (error.status === 401) setSession(null);
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

export async function request(
  path,
  options = {},
  retry = true,
  csrfRetry = true,
) {
  const method = options.method || "GET";
  const headers = { ...options.headers };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (accessToken && !path.startsWith("/auth/"))
    headers.Authorization = `Bearer ${accessToken}`;
  if (accessToken && path === "/auth/me")
    headers.Authorization = `Bearer ${accessToken}`;
  if (path.startsWith("/auth/") && method !== "GET")
    headers["X-XSRF-TOKEN"] = await csrf();
  let response;
  try {
    response = await fetch(`${base}${path}`, {
      ...options,
      method,
      headers,
      credentials: "include",
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new ApiError(
      "Cannot reach the server. Check your connection and try again.",
      0,
    );
  }
  if (response.status === 401 && retry && !path.startsWith("/auth/")) {
    await refreshSession();
    return request(path, options, false);
  }
  if (
    response.status === 403 &&
    csrfRetry &&
    path.startsWith("/auth/") &&
    method !== "GET"
  ) {
    // A concurrent bearer request may clear the cookie after bootstrap. A
    // rejected CSRF request has not reached the controller; retry it once.
    return request(path, options, retry, false);
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    if (response.status === 403 && path.startsWith("/auth/"))
      csrfPromise = null;
    throw new ApiError(
      error.message || `Request failed (${response.status})`,
      response.status,
      error.fields,
    );
  }
  return response.status === 204 ? null : response.json();
}

export const query = (values) => {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined)
      params.set(key, value);
  });
  return `?${params}`;
};
