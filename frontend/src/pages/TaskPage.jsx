import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Trash2, Pencil, Check } from "lucide-react";
import { labelsApi, organizationsApi, projectsApi, tasksApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useResource } from "../hooks/useResource";
import {
  canEditTask,
  canManage,
  dateTime,
  humanize,
  priorities,
  transitions,
} from "../utils/permissions";
import {
  Avatar,
  Badge,
  ErrorMessage,
  Field,
  Loading,
  Modal,
} from "../components/UI";
import Comments from "../features/Comments";
import Activity from "../features/Activity";
export default function TaskPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [revision, setRevision] = useState(0);
  const resource = useResource(async () => {
    const task = await tasksApi.get(id);
    const [project, members, labels] = await Promise.all([
      projectsApi.get(task.projectId),
      organizationsApi.members(task.organizationId),
      labelsApi.list(task.projectId),
    ]);
    return { task, project, members, labels };
  }, [id, revision]);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState(false);
  const [tab, setTab] = useState("comments");
  const refresh = () => setRevision((value) => value + 1);
  async function perform(action) {
    setBusy(true);
    setError(null);
    try {
      await action();
      setEditing(false);
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
  const { task, project, members, labels } = resource.data;
  const writable = project.status === "ACTIVE";
  const manage = writable && canManage(project.role);
  const edit = writable && canEditTask(project.role, user, task);
  const update = (values) =>
    perform(() => tasksApi.update(id, { ...values, version: task.version }));
  return (
    <div className="page">
      <Link className="back-link" to={`/projects/${project.id}`}>
        <ArrowLeft size={15} /> {project.name}
      </Link>
      <div className="page-heading">
        <div>
          <div className="task-heading-meta">
            <span className="task-id">{task.identifier}</span>
            <Badge tone={task.status}>{humanize(task.status)}</Badge>
          </div>
          <h1 className="task-title">{task.title}</h1>
        </div>
        <div className="actions">
          {edit && (
            <button onClick={() => setEditing(true)}>
              <Pencil size={15} /> Edit task
            </button>
          )}
          {manage && (
            <button
              className="icon-button danger"
              aria-label="Delete task"
              disabled={busy}
              onClick={async () => {
                if (
                  window.confirm(
                    "Permanently delete this task and its comments?",
                  )
                ) {
                  setBusy(true);
                  try {
                    await tasksApi.remove(id);
                    navigate(`/projects/${project.id}`);
                  } catch (failure) {
                    setError(failure);
                  } finally {
                    setBusy(false);
                  }
                }
              }}
            >
              <Trash2 size={17} />
            </button>
          )}
        </div>
      </div>
      <ErrorMessage error={error || resource.error} />
      {!writable && (
        <div className="notice">
          This project is archived. Its tasks are read-only.
        </div>
      )}
      <div className="task-layout">
        <div className="task-main">
          <section className="panel">
            <h2>Description</h2>
            <p className={`preserve-lines ${task.description ? "" : "muted"}`}>
              {task.description ||
                "No description yet. Add context to help your team get started."}
            </p>
            {task.labels.length > 0 && (
              <div className="labels">
                {task.labels.map((label) => (
                  <span key={label.id} className="label-chip">
                    <i style={{ background: label.color }} />
                    {label.name}
                  </span>
                ))}
              </div>
            )}
          </section>
          <div className="tabs">
            <button
              className={tab === "comments" ? "active" : ""}
              onClick={() => setTab("comments")}
            >
              Discussion
            </button>
            <button
              className={tab === "activity" ? "active" : ""}
              onClick={() => setTab("activity")}
            >
              Activity
            </button>
          </div>
          {tab === "comments" ? (
            <Comments
              key={id}
              taskId={id}
              role={project.role}
              writable={writable}
              onChange={refresh}
            />
          ) : (
            <Activity scope="tasks" id={id} revision={revision} />
          )}
        </div>
        <aside className="task-properties">
          <h2>Task properties</h2>
          <Field label="Status">
            <select
              aria-label="Task status"
              value={task.status}
              disabled={!edit || busy}
              onChange={(e) => update({ status: e.target.value })}
            >
              <option value={task.status}>{humanize(task.status)}</option>
              {transitions[task.status].map((status) => (
                <option key={status} value={status}>
                  {humanize(status)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Priority">
            <select
              aria-label="Task priority"
              value={task.priority}
              disabled={!manage || busy}
              onChange={(e) => update({ priority: e.target.value })}
            >
              {priorities.map((priority) => (
                <option key={priority} value={priority}>
                  {humanize(priority)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Assignee">
            <select
              aria-label="Task assignee"
              value={task.assignee?.id || ""}
              disabled={!manage || busy}
              onChange={(e) => update({ assigneeId: e.target.value || null })}
            >
              <option value="">Unassigned</option>
              {members.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.displayName}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Due date">
            <input
              aria-label="Task due date"
              type="date"
              value={task.dueDate || ""}
              disabled={!manage || busy}
              onChange={(e) => update({ dueDate: e.target.value || null })}
            />
          </Field>
          <div className="property">
            <span>Reported by</span>
            <div className="person">
              <Avatar name={task.reporter.displayName} />
              {task.reporter.displayName}
            </div>
          </div>
          <div className="property">
            <span>Created</span>
            <time>{dateTime(task.createdAt)}</time>
          </div>
          <div className="property">
            <span>Updated</span>
            <time>{dateTime(task.updatedAt)}</time>
          </div>
          {manage && (
            <div className="property">
              <span>Labels</span>
              {labels.length ? (
                <div className="label-picker">
                  {labels.map((label) => {
                    const selected = task.labels.some(
                      (item) => item.id === label.id,
                    );
                    return (
                      <button
                        key={label.id}
                        className={selected ? "selected" : ""}
                        disabled={busy}
                        aria-pressed={selected}
                        onClick={() =>
                          perform(() =>
                            selected
                              ? labelsApi.detach(id, label.id)
                              : labelsApi.attach(id, label.id),
                          )
                        }
                      >
                        <i style={{ background: label.color }} />
                        {label.name}
                        {selected && <Check size={13} />}
                      </button>
                    );
                  })}
                </div>
              ) : (
                <small className="muted">
                  Create labels from the project board.
                </small>
              )}
            </div>
          )}
        </aside>
      </div>
      {editing && (
        <Modal
          title={`Edit ${task.identifier}`}
          onClose={() => setEditing(false)}
        >
          <form
            onSubmit={(event) => {
              event.preventDefault();
              update(Object.fromEntries(new FormData(event.currentTarget)));
            }}
          >
            <ErrorMessage error={error} />
            <Field label="Task title">
              <input
                name="title"
                defaultValue={task.title}
                maxLength={200}
                required
              />
            </Field>
            <Field label="Description">
              <textarea
                name="description"
                defaultValue={task.description || ""}
                maxLength={10000}
                rows={6}
              />
            </Field>
            <button className="primary" disabled={busy}>
              Save changes
            </button>
          </form>
        </Modal>
      )}
    </div>
  );
}
