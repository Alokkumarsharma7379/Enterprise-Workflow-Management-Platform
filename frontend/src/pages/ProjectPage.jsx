import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Plus, Columns3, List, Search, Settings2, Tag } from "lucide-react";
import { labelsApi, organizationsApi, projectsApi, tasksApi } from "../api";
import { useResource } from "../hooks/useResource";
import {
  canAdmin,
  canManage,
  humanize,
  priorities,
  statuses,
} from "../utils/permissions";
import {
  Avatar,
  Badge,
  Empty,
  ErrorMessage,
  Field,
  Loading,
  Modal,
  Pagination,
} from "../components/UI";
import TaskForm from "../features/TaskForm";
import TaskCard from "../features/TaskCard";
import Activity from "../features/Activity";
export default function ProjectPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [revision, setRevision] = useState(0);
  const resource = useResource(async () => {
    const project = await projectsApi.get(id);
    const [members, labels, statistics] = await Promise.all([
      organizationsApi.members(project.organizationId),
      labelsApi.list(id),
      projectsApi.statistics(id),
    ]);
    return { project, members, labels, statistics };
  }, [id, revision]);
  const [filters, setFilters] = useState({
    search: "",
    status: "",
    priority: "",
    assigneeId: "",
    labelId: "",
    sort: "createdAt,desc",
    page: 0,
    size: 20,
  });
  const tasks = useResource(
    () => tasksApi.list(id, filters),
    [id, JSON.stringify(filters), revision],
  );
  const [search, setSearch] = useState("");
  const [view, setView] = useState("board");
  const [modal, setModal] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const refresh = () => setRevision((value) => value + 1);
  const filter = (key, value) =>
    setFilters((current) => ({ ...current, [key]: value, page: 0 }));
  async function perform(action) {
    setBusy(true);
    setError(null);
    try {
      await action();
      setModal(null);
      refresh();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }
  if (!resource.data)
    return (
      <div className="page">
        <ErrorMessage error={resource.error} />
        {resource.loading && <Loading />}
      </div>
    );
  const { project, members, labels, statistics } = resource.data;
  const manage = canManage(project.role) && project.status === "ACTIVE";
  const admin = canAdmin(project.role);
  return (
    <div className="page project-page">
      <div className="breadcrumbs">
        <Link to={`/organizations/${project.organizationId}`}>
          Organization
        </Link>
        <span>/</span>
        <span>{project.key}</span>
      </div>
      <div className="page-heading">
        <div>
          <span className="eyebrow">PROJECT WORKSPACE</span>
          <h1>{project.name}</h1>
          <p className="muted">
            {project.description || "Every next step, in one place."}
          </p>
        </div>
        <div className="actions">
          {admin && (
            <button
              onClick={() => {
                setError(null);
                setModal("settings");
              }}
            >
              <Settings2 size={16} /> Settings
            </button>
          )}
          {manage && (
            <button className="primary" onClick={() => setModal("task")}>
              <Plus size={17} /> New task
            </button>
          )}
        </div>
      </div>
      {project.status === "ARCHIVED" && (
        <div className="notice">
          This project is archived. An owner or admin can restore it in
          Settings.
        </div>
      )}
      <div className="stats-grid">
        <div className="stat">
          <span>Total tasks</span>
          <strong>{statistics.totalTasks}</strong>
          <small>Across this project</small>
        </div>
        {["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"].map((status) => (
          <div className="stat" key={status}>
            <span>
              <i className={`status-dot ${status.toLowerCase()}`} />
              {humanize(status)}
            </span>
            <strong>{statistics.byStatus[status]}</strong>
            <small>
              {status === "DONE" ? "Ready to celebrate" : "Current work"}
            </small>
          </div>
        ))}
      </div>
      <div className="project-toolbar">
        <div className="segmented">
          <button
            className={view === "board" ? "active" : ""}
            onClick={() => setView("board")}
          >
            <Columns3 size={16} /> Board
          </button>
          <button
            className={view === "list" ? "active" : ""}
            onClick={() => setView("list")}
          >
            <List size={16} /> List
          </button>
          <button
            className={view === "activity" ? "active" : ""}
            onClick={() => setView("activity")}
          >
            Activity
          </button>
        </div>
        {manage && (
          <button
            className="text-button"
            onClick={() => {
              setError(null);
              setModal("labels");
            }}
          >
            <Tag size={15} /> Manage labels
          </button>
        )}
      </div>
      <ErrorMessage error={resource.error} />
      {view === "activity" ? (
        <Activity scope="projects" id={id} revision={revision} />
      ) : (
        <>
          <div className="filters">
            <form
              className="search-field"
              onSubmit={(event) => {
                event.preventDefault();
                filter("search", search);
              }}
            >
              <Search size={16} />
              <input
                aria-label="Search tasks"
                placeholder="Search tasks…"
                value={search}
                maxLength={200}
                onChange={(event) => setSearch(event.target.value)}
              />
              <button type="submit">Search</button>
            </form>
            <select
              aria-label="Filter status"
              value={filters.status}
              onChange={(e) => filter("status", e.target.value)}
            >
              <option value="">All statuses</option>
              {statuses.map((s) => (
                <option key={s} value={s}>
                  {humanize(s)}
                </option>
              ))}
            </select>
            <select
              aria-label="Filter priority"
              value={filters.priority}
              onChange={(e) => filter("priority", e.target.value)}
            >
              <option value="">All priorities</option>
              {priorities.map((s) => (
                <option key={s} value={s}>
                  {humanize(s)}
                </option>
              ))}
            </select>
            <select
              aria-label="Filter assignee"
              value={filters.assigneeId}
              onChange={(e) => filter("assigneeId", e.target.value)}
            >
              <option value="">All assignees</option>
              {members.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.displayName}
                </option>
              ))}
            </select>
            <select
              aria-label="Filter label"
              value={filters.labelId}
              onChange={(e) => filter("labelId", e.target.value)}
            >
              <option value="">All labels</option>
              {labels.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name}
                </option>
              ))}
            </select>
            <select
              aria-label="Sort tasks"
              value={filters.sort}
              onChange={(e) => filter("sort", e.target.value)}
            >
              <option value="createdAt,desc">Newest first</option>
              <option value="createdAt,asc">Oldest first</option>
              <option value="updatedAt,desc">Recently updated</option>
              <option value="priority,desc">Highest priority</option>
              <option value="dueDate,asc">Due date</option>
            </select>
          </div>
          <div className="result-count">
            {tasks.data?.totalElements || 0} matching tasks · Showing{" "}
            {tasks.data?.content.length || 0} on this page
            {view === "board" && " · Board columns reflect this page"}
          </div>
          <ErrorMessage error={tasks.error} />
          {tasks.loading ? (
            <Loading />
          ) : view === "board" ? (
            <div className="board">
              {statuses.map((status) => {
                const column =
                  tasks.data?.content.filter(
                    (task) => task.status === status,
                  ) || [];
                return (
                  <section className="board-column" key={status}>
                    <div className="column-heading">
                      <i className={`status-dot ${status.toLowerCase()}`} />
                      <h2>{humanize(status)}</h2>
                      <span className="count">{column.length}</span>
                    </div>
                    <div className="column-content">
                      {column.map((task) => (
                        <TaskCard key={task.id} task={task} />
                      ))}
                      {!column.length && (
                        <p className="column-empty">No tasks on this page</p>
                      )}
                    </div>
                  </section>
                );
              })}
            </div>
          ) : tasks.data?.content.length ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Task</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Assignee</th>
                    <th>Due date</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.data.content.map((task) => (
                    <tr key={task.id}>
                      <td>
                        <Link to={`/tasks/${task.id}`}>
                          <span className="task-id">{task.identifier}</span>{" "}
                          {task.title}
                        </Link>
                      </td>
                      <td>
                        <Badge tone={task.status}>
                          {humanize(task.status)}
                        </Badge>
                      </td>
                      <td>
                        <Badge tone={task.priority}>
                          {humanize(task.priority)}
                        </Badge>
                      </td>
                      <td>
                        {task.assignee ? (
                          <span className="person">
                            <Avatar name={task.assignee.displayName} />
                            {task.assignee.displayName}
                          </span>
                        ) : (
                          "Unassigned"
                        )}
                      </td>
                      <td>{task.dueDate || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Empty title="No matching tasks">
              Try another filter or create a task to get started.
            </Empty>
          )}
          <Pagination
            result={tasks.data}
            onPage={(page) => setFilters((current) => ({ ...current, page }))}
          />
        </>
      )}
      {modal === "task" && (
        <Modal title="Create task" onClose={() => setModal(null)}>
          <TaskForm
            projectId={id}
            members={members}
            onCreated={() => {
              setModal(null);
              refresh();
            }}
          />
        </Modal>
      )}
      {modal === "settings" && (
        <Modal title="Project settings" onClose={() => setModal(null)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const values = Object.fromEntries(new FormData(e.currentTarget));
              perform(() =>
                projectsApi.update(id, { ...values, version: project.version }),
              );
            }}
          >
            <ErrorMessage error={error} />
            <Field label="Project name">
              <input
                name="name"
                defaultValue={project.name}
                maxLength={120}
                required
              />
            </Field>
            <Field label="Description">
              <textarea
                name="description"
                defaultValue={project.description || ""}
                maxLength={10000}
              />
            </Field>
            <Field label="Project status">
              <select name="status" defaultValue={project.status}>
                <option>ACTIVE</option>
                <option>ARCHIVED</option>
              </select>
            </Field>
            <button className="primary" disabled={busy}>
              Save changes
            </button>
          </form>
          <div className="danger-zone">
            <p>
              Deleting a project permanently removes its tasks and comments.
            </p>
            <button
              className="danger"
              disabled={busy}
              onClick={async () => {
                if (
                  window.confirm(
                    "Permanently delete this project and its tasks?",
                  )
                ) {
                  setBusy(true);
                  try {
                    await projectsApi.remove(id);
                    navigate(`/organizations/${project.organizationId}`);
                  } catch (failure) {
                    setError(failure);
                  } finally {
                    setBusy(false);
                  }
                }
              }}
            >
              Delete project
            </button>
          </div>
        </Modal>
      )}
      {modal === "labels" && (
        <Modal title="Project labels" onClose={() => setModal(null)}>
          <ErrorMessage error={error} />
          <div className="label-manager">
            {labels.map((label) => (
              <div key={label.id}>
                <span className="label-chip">
                  <i style={{ background: label.color }} />
                  {label.name}
                </span>
                <button
                  className="text-button"
                  disabled={busy}
                  onClick={() => {
                    const name = window.prompt("Label name", label.name);
                    if (name)
                      perform(() => labelsApi.update(label.id, { name }));
                  }}
                >
                  Rename
                </button>
                <button
                  className="text-button danger"
                  disabled={busy}
                  onClick={() => {
                    if (window.confirm(`Delete label ${label.name}?`))
                      perform(() => labelsApi.remove(label.id));
                  }}
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const values = Object.fromEntries(new FormData(e.currentTarget));
              perform(() => labelsApi.create(id, values));
            }}
          >
            <Field label="Label name">
              <input name="name" maxLength={50} required />
            </Field>
            <Field label="Color">
              <input name="color" type="color" defaultValue="#557b69" />
            </Field>
            <button className="primary" disabled={busy}>
              Create label
            </button>
          </form>
        </Modal>
      )}
    </div>
  );
}
