export const canManage = (role) => ["OWNER", "ADMIN", "MANAGER"].includes(role);
export const canAdmin = (role) => ["OWNER", "ADMIN"].includes(role);
export const canEditTask = (role, user, task) =>
  canManage(role) || task.assignee?.id === user?.id;
export const transitions = {
  TODO: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["TODO", "IN_REVIEW", "CANCELLED"],
  IN_REVIEW: ["IN_PROGRESS", "DONE", "CANCELLED"],
  DONE: [],
  CANCELLED: [],
};
export const statuses = [
  "TODO",
  "IN_PROGRESS",
  "IN_REVIEW",
  "DONE",
  "CANCELLED",
];
export const priorities = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
export const humanize = (text) =>
  text
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (c) => c.toUpperCase());
export const dateTime = (value) =>
  new Date(value).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
